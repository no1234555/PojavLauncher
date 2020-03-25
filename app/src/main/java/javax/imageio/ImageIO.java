package javax.imageio;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.*;
import java.awt.mod.*;
import net.kdt.pojavlaunch.*;

public class ImageIO {
    public static void setUseCache(boolean set) {
    }

    public static BufferedImage read(InputStream is) throws IOException {
        return makeBufferedImage(BitmapFactory.decodeStream(is));
    }

    public static BufferedImage read(File input) throws IOException {
		if (!input.exists()) {
			throw new FileNotFoundException(input.getAbsolutePath());
		}
		System.out.println(input.getAbsolutePath());
        return makeBufferedImage(BitmapFactory.decodeFile(input.getAbsolutePath()));
    }

    public static BufferedImage read(URL input) throws IOException {
        InputStream is = input.openStream();
        Bitmap bmp = BitmapFactory.decodeStream(is);
        is.close();
        return makeBufferedImage(bmp);
    }

    private static BufferedImage makeBufferedImage(Bitmap bmp) {
        return new BufferedImage(bmp);
    }

	public static boolean write(RenderedImage renderedImage, String str, File file) throws IOException {
		// MOD: Modified to fix blank image result and compatible with Android.
		System.out.println("ImageIO.write stub " + file);
		
		if (file == null) {
            throw new IllegalArgumentException("output cannot be NULL");
        }
        if (file.exists()) {
            file.delete();
        }
        if (renderedImage instanceof BufferedImage) {
            try {
				if (file.getParentFile() == null || !file.getParentFile().exists()) {
					file = new File(System.getProperty("user.home", Tools.MAIN_PATH), file.getAbsolutePath());
				}
                FileOutputStream fileOutputStream = new FileOutputStream(file);
				Bitmap.CompressFormat format = null;
				if (str.equalsIgnoreCase("jpg")) format = Bitmap.CompressFormat.JPEG;
				if (str.equalsIgnoreCase("png")) format = Bitmap.CompressFormat.PNG;
				// if (str.equalsIgnoreCase("gif")) format = Bitmap.CompressFormat.GIF;
             	boolean rt = ModdingKit.bufferToBitmap((BufferedImage) renderedImage).compress(format, 100, fileOutputStream);
				fileOutputStream.close();
                return rt;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}

