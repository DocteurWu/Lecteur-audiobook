package com.chapitres.podcast;

import java.io.File;
import java.io.RandomAccessFile;

/**
 * Lecture de la durée d'un fichier MP3 directement dans son en-tête :
 * beaucoup plus rapide qu'un MediaMetadataRetriever (71 fichiers en quelques ms).
 * Gère le CBR, le VBR (en-tête Xing/Info) et les tags ID3v1/ID3v2.
 */
public class Mp3Info {

    private static final int[] BITRATE_V1_L3 = {0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 0};
    private static final int[] BITRATE_V2_L3 = {0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, 0};
    private static final int[] SR_V1 = {44100, 48000, 32000, 0};
    private static final int[] SR_V2 = {22050, 24000, 16000, 0};
    private static final int[] SR_V25 = {11025, 12000, 8000, 0};

    private Mp3Info() {
    }

    /** Durée en millisecondes, 0 si le fichier n'est pas un MP3 exploitable. */
    public static long durationMs(File file) {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "r");
            long len = raf.length();
            if (len < 256) return 0;

            long audioStart = 0;
            byte[] h = new byte[10];
            raf.seek(0);
            raf.readFully(h);
            if (h[0] == 'I' && h[1] == 'D' && h[2] == '3') {
                int flags = h[5] & 0xFF;
                int size = ((h[6] & 0x7F) << 21) | ((h[7] & 0x7F) << 14) | ((h[8] & 0x7F) << 7) | (h[9] & 0x7F);
                audioStart = 10L + size;
                if ((flags & 0x10) != 0) audioStart += 10; // footer ID3v2.4
            }

            long audioEnd = len;
            raf.seek(len - 128);
            byte[] tail = new byte[3];
            raf.readFully(tail);
            if (tail[0] == 'T' && tail[1] == 'A' && tail[2] == 'G') audioEnd = len - 128;

            raf.seek(audioStart);
            byte[] buf = new byte[16384];
            int n = raf.read(buf);
            if (n < 8) return 0;

            int idx = findFrame(buf, n);
            if (idx < 0) return 0;

            int b2 = buf[idx + 1] & 0xFF;
            int b3 = buf[idx + 2] & 0xFF;
            int b4 = buf[idx + 3] & 0xFF;
            int verBits = (b2 >> 3) & 0x03;   // 3 = MPEG1, 2 = MPEG2, 0 = MPEG2.5
            int layerBits = (b2 >> 1) & 0x03; // 1 = Layer III
            int bitrateIdx = (b3 >> 4) & 0x0F;
            int srIdx = (b3 >> 2) & 0x03;
            int channelMode = (b4 >> 6) & 0x03;
            if (verBits == 1 || layerBits == 0) return 0;

            int[] brTable = (verBits == 3) ? BITRATE_V1_L3 : BITRATE_V2_L3;
            int[] srTable = (verBits == 3) ? SR_V1 : (verBits == 2 ? SR_V2 : SR_V25);
            int bitrate = brTable[bitrateIdx];
            int sampleRate = srTable[srIdx];
            if (bitrate <= 0 || sampleRate <= 0) return 0;

            int samplesPerFrame = (verBits == 3) ? 1152 : 576;
            boolean mono = (channelMode == 3);

            // En-tête Xing/Info : nombre de frames exact
            int sideInfo = (verBits == 3) ? (mono ? 17 : 32) : (mono ? 9 : 17);
            int xing = idx + 4 + sideInfo;
            if (xing + 12 <= n) {
                String magic = new String(buf, xing, 4, "ISO-8859-1");
                if (magic.equals("Xing") || magic.equals("Info")) {
                    int flags = ((buf[xing + 4] & 0xFF) << 24) | ((buf[xing + 5] & 0xFF) << 16)
                            | ((buf[xing + 6] & 0xFF) << 8) | (buf[xing + 7] & 0xFF);
                    if ((flags & 0x01) != 0) {
                        int frames = ((buf[xing + 8] & 0xFF) << 24) | ((buf[xing + 9] & 0xFF) << 16)
                                | ((buf[xing + 10] & 0xFF) << 8) | (buf[xing + 11] & 0xFF);
                        if (frames > 0) {
                            return (long) frames * samplesPerFrame * 1000L / sampleRate;
                        }
                    }
                }
            }

            // CBR
            double audioBytes = (double) (audioEnd - audioStart);
            if (audioBytes <= 0) return 0;
            return (long) (audioBytes * 8.0 * 1000.0 / (bitrate * 1000.0));
        } catch (Exception e) {
            return 0;
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static int findFrame(byte[] buf, int n) {
        for (int i = 0; i < n - 4; i++) {
            if ((buf[i] & 0xFF) != 0xFF) continue;
            if ((buf[i + 1] & 0xE0) != 0xE0) continue;
            int verBits = (buf[i + 1] >> 3) & 0x03;
            int layerBits = (buf[i + 1] >> 1) & 0x03;
            int bitrateIdx = (buf[i + 2] >> 4) & 0x0F;
            int srIdx = (buf[i + 2] >> 2) & 0x03;
            if (verBits == 1 || layerBits == 0) continue;
            if (bitrateIdx == 0 || bitrateIdx == 15 || srIdx == 3) continue;
            return i;
        }
        return -1;
    }
}
