/*
 * LunaClient - A best client on world.
 *  Copyright (C) 2024 Team PaichaLover
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.lunaclient.lunaclientmod.gui;

import cc.polyfrost.oneconfig.utils.Multithreading;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.FileResourcePack;
import net.minecraft.client.resources.FolderResourcePack;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.crash.CrashReport;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.ProgressManager.ProgressBar;
import net.minecraftforge.fml.common.asm.FMLSanityChecker;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.Drawable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.SharedDrawable;
import org.lwjgl.util.glu.GLU;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_BGRA;
import static org.lwjgl.opengl.GL12.GL_UNSIGNED_INT_8_8_8_8_REV;

/**
 * Not a fully fleshed out API, may change in future MC versions.
 * However feel free to use and suggest additions.
 */
public class CustomSplashProgress {
    public static final Semaphore mutex = new Semaphore(1);
    private static final Lock lock = new ReentrantLock(true);
    private static final IResourcePack mcPack = Minecraft.getMinecraft().mcDefaultResourcePack;
    private static final IResourcePack fmlPack = createResourcePack(FMLSanityChecker.fmlLocation);
    private static final IntBuffer buf = BufferUtils.createIntBuffer(4 * 1024 * 1024);
    private static String funFact = null;
    private static Drawable d;
    private static volatile boolean pause = false;
    private static volatile boolean done = false;
    private static Thread thread;
    private static volatile Throwable threadError;
    private static SplashFontRenderer fontRenderer;
    private static IResourcePack miscPack;
    private static Texture fontTexture;
    private static Texture logoTexture;
    private static Properties config;
    private static boolean enabled;
    private static boolean fullBars;
    private static int backgroundColor;
    private static int fontColor;
    private static int barBorderColor;
    private static int barColor;
    private static int barBackgroundColor;
    private static boolean showMemory;
    private static int memoryGoodColor;
    private static int memoryWarnColor;
    private static int memoryLowColor;
    private static float memoryColorPercent;
    private static long memoryColorChangeTime;
    private static int max_texture_size = -1;

    private static String getString(String name, String def) {
        String value = config.getProperty(name, def);
        config.setProperty(name, value);
        return value;
    }

    private static boolean getBool(String name, boolean def) {
        return Boolean.parseBoolean(getString(name, Boolean.toString(def)));
    }

    public static void start() {
        File configFile = new File(Minecraft.getMinecraft().mcDataDir, "config/splash.properties");

        File parent = configFile.getParentFile();
        if (!parent.exists()) parent.mkdirs();

        FileReader r = null;
        config = new Properties();
        try {
            r = new FileReader(configFile);
            config.load(r);
        } catch (IOException e) {
            FMLLog.info("Could not load splash.properties, will create a default one");
        } finally {
            IOUtils.closeQuietly(r);
        }

        //Some system do not support this and have weird effects so we need to detect and disable by default.
        //The user can always force enable it if they want to take the responsibility for bugs.
        //For now macs derp so disable them.
        boolean defaultEnabled = !System.getProperty("os.name").toLowerCase().contains("mac");

        // Enable if we have the flag, and there's either no optifine, or optifine has added a key to the blackboard ("optifine.ForgeSplashCompatible")
        // Optifine authors - add this key to the blackboard if you feel your modifications are now compatible with this code.
        enabled = getBool("enabled", defaultEnabled) && ((!FMLClientHandler.instance().hasOptifine()) || Launch.blackboard.containsKey("optifine.ForgeSplashCompatible"));
        fullBars = getBool("fullBars", false);
        showMemory = true;
        backgroundColor = 0xFFFFFF;
        fontColor = 0xFFFFFF;
        barBorderColor = 0x212121;
        barColor = 0xCB3D35;
        barBackgroundColor = 0x212121;
        memoryGoodColor = 0x78CB34;
        memoryWarnColor = 0xE6E84A;
        memoryLowColor = 0xE42F2F;

        final ResourceLocation fontLoc = new ResourceLocation(getString("fontTexture", "textures/font/ascii.png"));
        final ResourceLocation logoLoc = new ResourceLocation("lunaclientmod", "lunaclient_logo.png");

        File miscPackFile = new File(Minecraft.getMinecraft().mcDataDir, getString("resourcePackPath", "resources"));

        FileWriter w = null;
        try {
            w = new FileWriter(configFile);
            config.store(w, "Splash screen properties (LC) - DO NOT MODIFY!");
        } catch (IOException e) {
            FMLLog.log(Level.ERROR, e, "Could not save the splash.properties file");
        } finally {
            IOUtils.closeQuietly(w);
        }

        miscPack = createResourcePack(miscPackFile);

        // getting debug info out of the way, while we still can
        FMLCommonHandler.instance().registerCrashCallable(new ICrashCallable() {
            public String call() {
                return "' Vendor: '" + glGetString(GL_VENDOR) + "' Version: '" + glGetString(GL_VERSION) + "' Renderer: '" + glGetString(GL_RENDERER) + "'";
            }

            public String getLabel() {
                return "GL info";
            }
        });
        CrashReport report = CrashReport.makeCrashReport(new Throwable() {
            @Override
            public String getMessage() {
                return "This is just a prompt for computer specs to be printed. THIS IS NOT A ERROR";
            }

            @Override
            public void printStackTrace(final PrintWriter s) {
                s.println(getMessage());
            }

            @Override
            public void printStackTrace(final PrintStream s) {
                s.println(getMessage());
            }
        }, "Loading screen debug info");
        System.out.println(report.getCompleteReport());

        try {
            d = new SharedDrawable(Display.getDrawable());
            Display.getDrawable().releaseContext();
            d.makeCurrent();
        } catch (LWJGLException e) {
            e.printStackTrace();
            disableSplash(e);
        }

        //Call this ASAP if splash is enabled so that threading doesn't cause issues later
        getMaxTextureSize();

        Multithreading.runAsync(() -> {
            try {
                String text = "LunaClient LLL\nlol";
                if (text != null) {
                    String[] lines = text.split("\n");
                    int index = (int) (Math.random() * lines.length);
                    funFact = lines[index];
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        //Thread mainThread = Thread.currentThread();
        thread = new Thread(new Runnable() {
            private final int barWidth = 400;
            private final int barHeight = 20;
            private final int textHeight2 = 20;

            public void run() {
                setGL();
                fontTexture = new Texture(fontLoc, null);
                logoTexture = new Texture(logoLoc, null, false);
                glEnable(GL_TEXTURE_2D);
                fontRenderer = new SplashFontRenderer();
                glDisable(GL_TEXTURE_2D);
                while (!done) {
                    ProgressBar first = null, penult = null, last = null;
                    Iterator<ProgressBar> i = ProgressManager.barIterator();
                    while (i.hasNext()) {
                        if (first == null) first = i.next();
                        else {
                            penult = last;
                            last = i.next();
                        }
                    }

                    glClear(GL_COLOR_BUFFER_BIT);

                    // matrix setup
                    int w = Display.getWidth();
                    int h = Display.getHeight();
                    glViewport(0, 0, w, h);
                    glMatrixMode(GL_PROJECTION);
                    glLoadIdentity();
                    glOrtho(320 - (w >> 1), 320 + (w >> 1), 240 + (h >> 1), 240 - (h >> 1), -1, 1);
                    glMatrixMode(GL_MODELVIEW);
                    glLoadIdentity();

                    // memory usage
                    if (showMemory) {
                        glPushMatrix();
                        glTranslatef(320 - (float) barWidth / 2, 20, 0);
                        drawMemoryBar();
                        glPopMatrix();
                    }

                    // logo
                    glPushMatrix();
                    setColor(backgroundColor);
                    glEnable(GL_TEXTURE_2D);
                    glScaled(0.5, 0.5, 0.5);
                    glTranslated(320, 160, 0);
                    logoTexture.bind();
                    glBegin(GL_QUADS);
                    logoTexture.texCoord(0, 0, 0);
                    glVertex2f(320 - 256, 240 - 256);
                    logoTexture.texCoord(0, 0, 1);
                    glVertex2f(320 - 256, 240 + 256);
                    logoTexture.texCoord(0, 1, 1);
                    glVertex2f(320 + 256, 240 + 256);
                    logoTexture.texCoord(0, 1, 0);
                    glVertex2f(320 + 256, 240 - 256);
                    glEnd();
                    glDisable(GL_TEXTURE_2D);
                    glPopMatrix();

                    if (funFact != null && !funFact.isEmpty()) {
                        glPushMatrix();
                        setColor(fontColor);
                        glScalef(2, 2, 1);
                        glEnable(GL_TEXTURE_2D);
                        int offset = 0;
                        for (String segment : funFact.split("\\\\n")) {
                            fontRenderer.drawString(segment, 160 - (fontRenderer.getStringWidth(segment) / 2), 180 - textHeight2 + offset, 0x000000);
                            offset += 10;
                        }
                        glDisable(GL_TEXTURE_2D);
                        glPopMatrix();
                    }

                    // bars
                    if (!fullBars) {
                        if (first != null) {
                            glPushMatrix();
                            int barOffset = 55;
                            glTranslatef(320 - (float) barWidth / 2, 310 + barOffset, 0);
                            drawBar(first);
                            if (penult != null) {
                                glTranslatef(0, barOffset, 0);
                                drawBar(penult);
                            }
                            glPopMatrix();
                        }
                    } else {
                        if (first != null) {
                            glPushMatrix();
                            glTranslatef(320 - (float) barWidth / 2, 310, 0);
                            drawBar(first);
                            int barOffset = 55;
                            if (penult != null) {
                                glTranslatef(0, barOffset, 0);
                                drawBar(penult);
                            }
                            if (last != null) {
                                glTranslatef(0, barOffset, 0);
                                drawBar(last);
                            }
                            glPopMatrix();
                        }
                    }


                    // We use mutex to indicate safely to the main thread that we're taking the display global lock
                    // So the main thread can skip processing messages while we're updating.
                    // There are system setups where this call can pause for a while, because the GL implementation
                    // is trying to impose a framerate or other thing is occurring. Without the mutex, the main
                    // thread would delay waiting for the same global display lock
                    mutex.acquireUninterruptibly();
                    Display.update();
                    // As soon as we're done, we release the mutex. The other thread can now ping the processmessages
                    // call as often as it wants until we get get back here again
                    mutex.release();
                    if (pause) {
                        clearGL();
                        setGL();
                    }
                    Display.sync(100);
                }
                clearGL();
            }

            private void setColor(int color) {
                glColor3ub((byte) ((color >> 16) & 0xFF), (byte) ((color >> 8) & 0xFF), (byte) (color & 0xFF));
            }

            private void drawBox(int w, int h) {
                glBegin(GL_QUADS);
                glVertex2f(0, 0);
                glVertex2f(0, h);
                glVertex2f(w, h);
                glVertex2f(w, 0);
                glEnd();
            }

            private void drawBar(ProgressBar b) {
                glPushMatrix();
                // title - message
                setColor(fontColor);
                glScalef(2, 2, 1);
                glEnable(GL_TEXTURE_2D);
                fontRenderer.drawString(b.getTitle() + " - " + b.getMessage(), 0, 0, 0x000000);
                glDisable(GL_TEXTURE_2D);
                glPopMatrix();
                // border
                glPushMatrix();
                glTranslatef(0, textHeight2, 0);
                setColor(barBorderColor);
                drawBox(barWidth, barHeight);
                // interior
                setColor(barBackgroundColor);
                glTranslatef(1, 1, 0);
                drawBox(barWidth - 2, barHeight - 2);
                // slidy part
                setColor(barColor);
                drawBox((barWidth - 2) * (b.getStep() + 1) / (b.getSteps() + 1), barHeight - 2); // Step can sometimes be 0.
                // progress text
                String progress = b.getStep() + "/" + b.getSteps();
                glTranslatef(((float) barWidth - 2) / 2 - fontRenderer.getStringWidth(progress), 2, 0);
                setColor(fontColor);
                glScalef(2, 2, 1);
                glEnable(GL_TEXTURE_2D);
                fontRenderer.drawString(progress, 0, 0, 0x000000);
                glPopMatrix();
            }

            private void drawMemoryBar() {
                int maxMemory = bytesToMb(Runtime.getRuntime().maxMemory());
                int totalMemory = bytesToMb(Runtime.getRuntime().totalMemory());
                int freeMemory = bytesToMb(Runtime.getRuntime().freeMemory());
                int usedMemory = totalMemory - freeMemory;
                float usedMemoryPercent = usedMemory / (float) maxMemory;
                glPushMatrix();
                // title - message
                setColor(fontColor);
                glScalef(2, 2, 1);
                glEnable(GL_TEXTURE_2D);
                fontRenderer.drawString("Memory Used / Total", 0, 0, 0x000000);
                glDisable(GL_TEXTURE_2D);
                glPopMatrix();
                // border
                glPushMatrix();
                glTranslatef(0, textHeight2, 0);
                setColor(barBorderColor);
                drawBox(barWidth, barHeight);
                // interior
                setColor(barBackgroundColor);
                glTranslatef(1, 1, 0);
                drawBox(barWidth - 2, barHeight - 2);
                // slidy part
                long time = System.currentTimeMillis();
                if (usedMemoryPercent > memoryColorPercent || (time - memoryColorChangeTime > 1000)) {
                    memoryColorChangeTime = time;
                    memoryColorPercent = usedMemoryPercent;
                }
                int memoryBarColor;
                if (memoryColorPercent < 0.75f) {
                    memoryBarColor = memoryGoodColor;
                } else if (memoryColorPercent < 0.85f) {
                    memoryBarColor = memoryWarnColor;
                } else {
                    memoryBarColor = memoryLowColor;
                }
                setColor(memoryLowColor);
                glPushMatrix();
                glTranslatef((barWidth - 2) * ((float) totalMemory) / (maxMemory) - 2, 0, 0);
                drawBox(2, barHeight - 2);
                glPopMatrix();
                setColor(memoryBarColor);
                drawBox((barWidth - 2) * (usedMemory) / (maxMemory), barHeight - 2);
                // progress text
                String progress = getMemoryString(usedMemory) + " / " + getMemoryString(maxMemory);
                glTranslatef(((float) barWidth - 2) / 2 - fontRenderer.getStringWidth(progress), 2, 0);
                setColor(fontColor);
                glScalef(2, 2, 1);
                glEnable(GL_TEXTURE_2D);
                fontRenderer.drawString(progress, 0, 0, 0x000000);
                glPopMatrix();
            }

            private String getMemoryString(int memory) {
                return StringUtils.leftPad(Integer.toString(memory), 4, ' ') + " MB";
            }

            private void setGL() {
                lock.lock();
                try {
                    Display.getDrawable().makeCurrent();
                } catch (LWJGLException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
                glClearColor((float) ((0) & 0xFF) / 0xFF, (float) ((0) & 0xFF) / 0xFF, (float) (0) / 0xFF, 1);
                glDisable(GL_LIGHTING);
                glDisable(GL_DEPTH_TEST);
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            }

            private void clearGL() {
                Minecraft mc = Minecraft.getMinecraft();
                mc.displayWidth = Display.getWidth();
                mc.displayHeight = Display.getHeight();
                mc.resize(mc.displayWidth, mc.displayHeight);
                glClearColor(1, 1, 1, 1);
                glEnable(GL_DEPTH_TEST);
                glDepthFunc(GL_LEQUAL);
                glEnable(GL_ALPHA_TEST);
                glAlphaFunc(GL_GREATER, .1f);
                try {
                    Display.getDrawable().releaseContext();
                } catch (LWJGLException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                } finally {
                    lock.unlock();
                }
            }
        });
        thread.setUncaughtExceptionHandler((t, e) -> {
            FMLLog.log(Level.ERROR, e, "Splash thread Exception");
            threadError = e;
        });
        thread.start();
        checkThreadState();
    }

    public static int getMaxTextureSize() {
        if (max_texture_size != -1) return max_texture_size;
        for (int i = 0x4000; i > 0; i >>= 1) {
            GL11.glTexImage2D(GL11.GL_PROXY_TEXTURE_2D, 0, GL11.GL_RGBA, i, i, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
            if (GL11.glGetTexLevelParameteri(GL11.GL_PROXY_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH) != 0) {
                max_texture_size = i;
                return i;
            }
        }
        return -1;
    }

    private static void checkThreadState() {
        if (thread.getState() == Thread.State.TERMINATED || threadError != null) {
            throw new IllegalStateException("Splash thread", threadError);
        }
    }

    /**
     * Call before you need to explicitly modify GL context state during loading.
     * Resource loading doesn't usually require this call.
     * Call {@link #resume()} when you're done.
     */
    public static void pause() {
        if (!enabled) return;
        checkThreadState();
        pause = true;
        lock.lock();
        try {
            d.releaseContext();
            Display.getDrawable().makeCurrent();
        } catch (LWJGLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void resume() {
        if (!enabled) return;
        checkThreadState();
        pause = false;
        try {
            Display.getDrawable().releaseContext();
            d.makeCurrent();
        } catch (LWJGLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        lock.unlock();
    }

    public static void finish() {
        if (!enabled) return;
        try {
            checkThreadState();
            done = true;
            thread.join();
            d.releaseContext();
            Display.getDrawable().makeCurrent();
            fontTexture.delete();
            logoTexture.delete();
        } catch (Exception e) {
            e.printStackTrace();
            disableSplash(e);
        }
    }

    private static boolean disableSplash(Exception e) {
        if (disableSplash()) {
            throw new EnhancedRuntimeException(e) {
                @Override
                protected void printStackTrace(WrappedPrintStream stream) {
                    stream.println("CustomSplashProgress has detected a error loading Minecraft.");
                    stream.println("This can sometimes be caused by bad video drivers.");
                    stream.println("We have automatically disabled the new Splash Screen in config/splash.properties.");
                    stream.println("Try reloading minecraft before reporting any errors.");
                }
            };
        } else {
            throw new EnhancedRuntimeException(e) {
                @Override
                protected void printStackTrace(WrappedPrintStream stream) {
                    stream.println("CustomSplashProgress has detected a error loading Minecraft.");
                    stream.println("This can sometimes be caused by bad video drivers.");
                    stream.println("Please try disabling the new Splash Screen in config/splash.properties.");
                    stream.println("After doing so, try reloading minecraft before reporting any errors.");
                }
            };
        }
    }

    private static boolean disableSplash() {
        File configFile = new File(Minecraft.getMinecraft().mcDataDir, "config/splash.properties");
        File parent = configFile.getParentFile();
        if (!parent.exists()) parent.mkdirs();

        enabled = false;
        config.setProperty("enabled", "false");

        FileWriter w = null;
        try {
            w = new FileWriter(configFile);
            config.store(w, "Splash screen properties");
        } catch (IOException e) {
            FMLLog.log(Level.ERROR, e, "Could not save the splash.properties file");
            return false;
        } finally {
            IOUtils.closeQuietly(w);
        }
        return true;
    }

    private static IResourcePack createResourcePack(File file) {
        if (file.isDirectory()) {
            return new FolderResourcePack(file);
        } else {
            return new FileResourcePack(file);
        }
    }

    public static void drawVanillaScreen(TextureManager renderEngine) throws LWJGLException {
        if (!enabled) {
            Minecraft.getMinecraft().drawSplashScreen(renderEngine);
        }
    }

    public static void clearVanillaResources(TextureManager renderEngine, ResourceLocation mojangLogo) {
        if (!enabled) {
            renderEngine.deleteTexture(mojangLogo);
        }
    }

    public static void checkGLError(String where) {
        int err = GL11.glGetError();
        if (err != 0) {
            throw new IllegalStateException(where + ": " + GLU.gluErrorString(err));
        }
    }

    private static InputStream open(ResourceLocation loc, ResourceLocation fallback, boolean allowRP) throws IOException {
        if (!allowRP) return mcPack.getInputStream(loc);
        if (miscPack.resourceExists(loc)) {
            return miscPack.getInputStream(loc);
        } else if (fmlPack.resourceExists(loc)) {
            return fmlPack.getInputStream(loc);
        } else if (!mcPack.resourceExists(loc) && fallback != null) {
            return open(fallback, null, true);
        }
        return mcPack.getInputStream(loc);
    }

    private static int bytesToMb(long bytes) {
        return (int) (bytes / 1024L / 1024L);
    }

    @SuppressWarnings("unused")
    private static class Texture {
        private final ResourceLocation location;
        private final int name;
        private final int width;
        private final int height;
        private final int frames;
        private final int size;

        public Texture(ResourceLocation location, ResourceLocation fallback) {
            this(location, fallback, true);
        }

        @SuppressWarnings("RedundantCast")
        public Texture(ResourceLocation location, ResourceLocation fallback, boolean allowRP) {
            InputStream s = null;
            try {
                this.location = location;
                s = open(location, fallback, allowRP);
                ImageInputStream stream = ImageIO.createImageInputStream(s);
                Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
                if (!readers.hasNext()) throw new IOException("No suitable reader found for image" + location);
                ImageReader reader = readers.next();
                reader.setInput(stream);
                int frames = reader.getNumImages(true);
                BufferedImage[] images = new BufferedImage[frames];
                for (int i = 0; i < frames; i++) {
                    images[i] = reader.read(i);
                }
                reader.dispose();
                width = images[0].getWidth();
                int height = images[0].getHeight();
                // Animation strip
                if (height > width && height % width == 0) {
                    frames = height / width;
                    BufferedImage original = images[0];
                    height = width;
                    images = new BufferedImage[frames];
                    for (int i = 0; i < frames; i++) {
                        images[i] = original.getSubimage(0, i * height, width, height);
                    }
                }
                this.frames = frames;
                this.height = height;
                int size = 1;
                while ((size / width) * (size / height) < frames) size *= 2;
                this.size = size;
                glEnable(GL_TEXTURE_2D);
                synchronized (CustomSplashProgress.class) {
                    name = glGenTextures();
                    glBindTexture(GL_TEXTURE_2D, name);
                }
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, size, size, 0, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, (IntBuffer) null);
                checkGLError("Texture creation");
                for (int i = 0; i * (size / width) < frames; i++) {
                    for (int j = 0; i * (size / width) + j < frames && j < size / width; j++) {
                        ((Buffer) buf).clear();
                        BufferedImage image = images[i * (size / width) + j];
                        for (int k = 0; k < height; k++) {
                            for (int l = 0; l < width; l++) {
                                buf.put(image.getRGB(l, k));
                            }
                        }
                        ((Buffer) buf).position(0).limit(width * height);
                        glTexSubImage2D(GL_TEXTURE_2D, 0, j * width, i * height, width, height, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, buf);
                        checkGLError("Texture uploading");
                    }
                }
                glBindTexture(GL_TEXTURE_2D, 0);
                glDisable(GL_TEXTURE_2D);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } finally {
                IOUtils.closeQuietly(s);
            }
        }

        public ResourceLocation getLocation() {
            return location;
        }

        public int getName() {
            return name;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getFrames() {
            return frames;
        }

        public int getSize() {
            return size;
        }

        public void bind() {
            glBindTexture(GL_TEXTURE_2D, name);
        }

        public void delete() {
            glDeleteTextures(name);
        }

        public float getU(int frame, float u) {
            return width * (frame % ((float) size / width) + u) / size;
        }

        public float getV(int frame, float v) {
            return height * (frame / ((float) size / width) + v) / size;
        }

        public void texCoord(int frame, float u, float v) {
            glTexCoord2f(getU(frame, u), getV(frame, v));
        }
    }

    private static class SplashFontRenderer extends FontRenderer {
        public SplashFontRenderer() {
            super(Minecraft.getMinecraft().gameSettings, fontTexture.getLocation(), null, false);
            super.onResourceManagerReload(null);
        }

        @Override
        protected void bindTexture(ResourceLocation location) {
            //if (location != locationFontTexture) throw new IllegalArgumentException();
            fontTexture.bind();
        }

        @Override
        protected InputStream getResourceInputStream(ResourceLocation location) throws IOException {
            return Minecraft.getMinecraft().mcDefaultResourcePack.getInputStream(location);
        }
    }
}
