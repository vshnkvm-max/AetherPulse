package dev.aetherpulse.monument;

import dev.aetherpulse.animation.*;
import dev.aetherpulse.config.Settings;
import dev.aetherpulse.visual.*;
import org.bukkit.*;
import org.bukkit.entity.TextDisplay;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

/** Procedural crystal scene. All entities are spawned once and animated by local matrices. */
public final class CrystalCore implements AutoCloseable {
    private record Part(VisualObject object, int group, int index) { }
    private final Monument monument;
    private final Settings settings;
    private final Location origin;
    private final VisualEngine scene;
    private final Transform root = new Transform();
    private final Transform[] rings = { new Transform(), new Transform(), new Transform() };
    private final List<Part> parts = new ArrayList<>();
    private final double[] radii, speeds;
    private final float[] visibility = new float[5];
    private final float[] from = new float[5];
    private final float[] to = new float[5];
    private final TextDisplay text;
    private final Animation coreAnimation;
    private final AnimationSequence ascension = new AnimationSequence(List.of(
            new AnimationSequence.Clip(1.2, (t, s) -> {
                t.position().y += (float) (.55 * Easing.EASE_OUT.apply(s / 1.2));
                t.scale().mul((float) (1 + .18 * Easing.EASE_IN.apply(s / 1.2)));
            }),
            new AnimationSequence.Clip(2.4, (t, s) -> {
                double f = Easing.SMOOTHSTEP.apply(s / 2.4);
                t.position().y += (float) (.55 * (1 - f));
                t.rotation().rotateY((float) (Math.PI * 2 * f));
                t.scale().mul((float) (1 + .18 * (1 - f)));
            })));
    private double ascensionStart = -10;
    private double elapsed, transitionStart = -10, reactionStart = -10;
    private final double[] ringAngles = new double[3];
    private double orbitAngle, orbitVerticalAngle, animationStage;
    private boolean levelBurst;
    private String lastText = "";
    public CrystalCore(Monument monument, Settings settings, NamespacedKey key) {
        this.monument = monument; this.settings = settings;
        animationStage = monument.stage().number();
        origin = monument.location().add(0, 2.1, 0);
        radii = settings.radii(); speeds = settings.speeds();
        scene = new VisualEngine(settings.entityLimit());
        coreAnimation = Animation.together(Animations.rotation(new Vector3f(0, 1, 0), .26),
                Animations.sine(.12, 1.2), Animations.pulse(.055, 2.1));
        DisplayFactory factory = new DisplayFactory(key, monument.id(), settings.animationInterval());
        TextDisplay createdText;
        try {
            for (int i = 0; i < 3; i++) rings[i].parent(root);
            add(factory, Material.AMETHYST_BLOCK, 0, 0, false);
            add(factory, Material.PURPUR_BLOCK, 0, 1, false);
            add(factory, Material.AMETHYST_BLOCK, 0, 2, false);
            add(factory, Material.END_CRYSTAL, 0, 3, true);
            for (int r = 0; r < 3; r++) for (int i = 0; i < settings.segments(); i++) {
                VisualObject o = add(factory, i % 4 == 0 ? Material.SEA_LANTERN : Material.PURPUR_BLOCK, r + 1, i, false);
                o.transform().parent(rings[r]);
            }
            for (int i = 0; i < settings.fragments(); i++) add(factory, Material.AMETHYST_SHARD, 4, i, true);
            createdText = factory.text(origin);
            VisualObject label = scene.add(new VisualObject(createdText, false));
            label.transform().position().set(0, 2.95f, 0); label.transform().scale().set(.65f);
            parts.add(new Part(label, -1, 0));
            scene.own(factory.interaction(origin.clone().add(0, -1.2, 0)));
            targets(monument.stage());
            System.arraycopy(to, 0, visibility, 0, 5);
            System.arraycopy(to, 0, from, 0, 5);
            text = createdText;
            update(0);
        } catch (RuntimeException e) { scene.close(); throw e; }
    }
    private VisualObject add(DisplayFactory factory, Material material, int group, int index, boolean item) {
        VisualObject o = scene.add(new VisualObject(item ? factory.item(origin, material) : factory.block(origin, material), !item));
        o.transform().parent(root);
        parts.add(new Part(o, group, index)); return o;
    }
    private void targets(MonumentStage stage) {
        to[0] = 1;
        for (int i = 1; i <= 3; i++) to[i] = stage.rings() >= i ? 1 : 0;
        to[4] = stage.number() >= 3 ? 1 : 0;
    }
    public void transition(MonumentStage old, MonumentStage stage) {
        System.arraycopy(visibility, 0, from, 0, 5); targets(stage); transitionStart = elapsed;
        levelBurst = false;
        if (stage != MonumentStage.ASCENDED) ascensionStart = -10;
        if (stage.number() > old.number()) {
            levelBurst = true;
            if (stage == MonumentStage.ASCENDED) ascensionStart = elapsed;
            if (settings.sounds()) origin.getWorld().playSound(origin, Sound.BLOCK_BEACON_POWER_SELECT, .8f, .8f + stage.number() * .08f);
        }
    }
    public void react() { reactionStart = elapsed; }
    public boolean valid() { return scene.valid(); }
    public int entityCount() { return scene.size() + 1; }
    public void update(double deltaSeconds) {
        elapsed += deltaSeconds;
        double s = elapsed;
        animationStage += (monument.stage().number() - animationStage) * Math.min(1, deltaSeconds * 2);
        double stage = animationStage;
        // Integrate angular velocity: changing stage must not reinterpret all elapsed time.
        for (int i = 0; i < 3; i++) ringAngles[i] = (ringAngles[i] + deltaSeconds * speeds[i] * (1 + stage * .12)) % (Math.PI * 2);
        double orbitalStep = deltaSeconds * (.35 + stage * .05);
        orbitAngle = (orbitAngle + orbitalStep) % (Math.PI * 2);
        orbitVerticalAngle = (orbitVerticalAngle + orbitalStep * 1.3) % (Math.PI * 2);
        float blend = (float) Easing.SMOOTHSTEP.apply((s - transitionStart) / 2.5);
        for (int i = 0; i < 5; i++) visibility[i] = from[i] + (to[i] - from[i]) * blend;
        root.position().zero(); root.rotation().identity(); root.scale().set(1);
        coreAnimation.apply(root, s);
        if (s - ascensionStart <= ascension.duration()) ascension.apply(root, s - ascensionStart);
        double cinematic = Math.max(0, 1 - (s - transitionStart) / 3.0);
        double reaction = Math.max(0, 1 - (s - reactionStart) / .6);
        root.scale().mul((float) (1 + .12 * cinematic * Math.sin((s - transitionStart) * 5) + .07 * reaction));
        for (int i = 0; i < 3; i++) {
            rings[i].rotation().identity().rotateXYZ((float) (.5 + i * .63), (float) ringAngles[i], (float) (i * .7));
            rings[i].position().zero(); rings[i].scale().set(1);
        }
        for (Part p : parts) {
            if (p.group == -1) { p.object.render(1); continue; }
            Transform t = p.object.transform();
            t.position().zero(); t.rotation().identity(); t.scale().set(1);
            if (p.group == 0) {
                float size = settings.coreSize();
                switch (p.index) {
                    case 0 -> { t.rotation().rotateXYZ(.62f, .78f, .62f); t.scale().set(size * .78f, size * 1.6f, size * .78f); }
                    case 1 -> { t.rotation().rotateXYZ(.78f, -.4f, .78f); t.scale().set(size * 1.12f, size * .72f, size * 1.12f); }
                    case 2 -> { t.position().y = .88f * size; t.rotation().rotateZ(.78f); t.scale().set(size * .4f); }
                    case 3 -> { t.scale().set(size * .5f); t.rotation().rotateY((float) -s * .5f); }
                    default -> throw new IllegalStateException();
                }
                t.scale().mul((float) (1 + .015 * stage * Math.sin(s * 2.5)));
            } else if (p.group <= 3) {
                double a = 2 * Math.PI * p.index / settings.segments();
                double radius = radii[p.group - 1];
                t.position().set((float) (Math.cos(a) * radius), 0, (float) (Math.sin(a) * radius));
                t.rotation().rotateY((float) -a);
                t.scale().set(.14f, .11f, (float) (2 * Math.PI * radius / settings.segments() * .67));
            } else {
                double phase = 2 * Math.PI * p.index / Math.max(1, settings.fragments());
                t.position().set(fragmentPosition(p.index));
                t.rotation().rotateXYZ((float) s, (float) (s * .7 + phase), .6f);
                t.scale().set(.4f);
            }
            p.object.render(visibility[p.group]);
        }
        String content = "§d§lCRYSTAL CORE\n§f" + monument.stage().title() + " §8• §7Stage " + monument.stage().number()
                + (monument.overridden() ? " §e[override]" : "") + "\n§d" + bar(settings.curve().fraction(monument.points()))
                + "\n§f" + monument.points() + " §7points";
        if (!content.equals(lastText)) { text.setText(content); lastText = content; }
    }
    private Vector3f fragmentPosition(int index) {
        double phase = 2 * Math.PI * index / Math.max(1, settings.fragments());
        double angle = orbitAngle + phase;
        return new Vector3f((float) ((radii[0] + .38) * Math.cos(angle)),
                (float) (.65 * Math.sin(orbitVerticalAngle + phase)),
                (float) ((radii[0] + .38) * Math.sin(angle)));
    }
    private String bar(double fraction) {
        int count = (int) Math.round(fraction * 16);
        return "§d" + "▰".repeat(count) + "§8" + "▱".repeat(16 - count);
    }
    /** Returns actual particle count, never exceeding the caller's global remaining budget. */
    public int particles(int budget) {
        if (!settings.particles() || budget <= 0 || settings.intensity() == 0) return 0;
        int cap = Math.min(budget, (int) (settings.intensity() * (5 + monument.stage().number() * 4)));
        int used = 0;
        World world = origin.getWorld();
        Particle.DustOptions dust = new Particle.DustOptions(settings.dustColor(), .8f);
        if (levelBurst && settings.bursts()) {
            int count = Math.min(cap, 24);
            world.spawnParticle(Particle.END_ROD, origin, count, .8, 1, .8, .04);
            used += count; levelBurst = false;
        }
        for (int i = used; i < cap; i++) {
            double phase = elapsed * 1.8 + i * 2.39996;
            Vector3f point = new Vector3f();
            if (i % 3 == 0 && monument.stage().rings() > 0) {
                int ring = (i / 3) % monument.stage().rings();
                point.set((float) (radii[ring] * Math.cos(phase)), 0, (float) (radii[ring] * Math.sin(phase)));
                rings[ring].worldMatrix().transformPosition(point);
            } else if (settings.streams() && settings.fragments() > 1 && monument.stage().number() >= 4 && i % 3 == 1) {
                float f = (float) ((elapsed * .65 + i * .13) % 1);
                int fragment = (i / 3) % settings.fragments();
                point.set(fragmentPosition(fragment)).lerp(fragmentPosition((fragment + 1) % settings.fragments()), f);
                root.worldMatrix().transformPosition(point);
            } else if (settings.spiral()) {
                float h = (float) ((elapsed * .45 + i * .17) % 2.7 - 1.35);
                point.set((float) (.7 * Math.cos(phase)), h, (float) (.7 * Math.sin(phase)));
                root.worldMatrix().transformPosition(point);
            } else continue;
            if (monument.stage() == MonumentStage.ASCENDED && i % 4 == 0)
                world.spawnParticle(Particle.END_ROD, origin.getX() + point.x, origin.getY() + point.y, origin.getZ() + point.z, 1, 0, 0, 0, 0);
            else world.spawnParticle(Particle.DUST, origin.getX() + point.x, origin.getY() + point.y, origin.getZ() + point.z, 1, 0, 0, 0, 0, dust);
            used++;
        }
        return used;
    }
    @Override public void close() { scene.close(); }
}
