package edu.wisc.physics.icecube.decov2;

/**
 * Created by Matthew on 7/28/2014.
 */

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class DataProcessor extends Thread
{
    private static final String TAG = "DataProcessor";

    private static final SimpleDateFormat IMAGE_FILE_FORMAT = new SimpleDateFormat("yyyyMMdd_hhmmss");

    // if you want to change the scale (which will have to be changed in the calibration protion of the set up (not done) <done
    private static final double SCALE_X = 11;
    private static final double SCALE_Y = 11;

    private static final int RED_THRESHOLD = 80;
    private static final int GREEN_THRESHOLD = 80;
    private static final int BLUE_THRESHOLD = 80;

    private static boolean longProcess = false;

    private ArrayList<byte[]> paths = new ArrayList<byte[]>();

    private boolean go = false;
    private boolean running = false;
    private boolean ready = true;

    public void setImage(byte[] bytes)
    {
        paths.add(bytes);
        go = true;
        if (!running)
        {
            go = true;
            running = true;
        }
        Log.d(TAG, paths.size() + " in Queue. is long running? " + longProcess);

        // Tell Camera2BasicFragment number in q so that/ uiupdater can update properly.
        Camera2BasicFragment.inQuaue = paths.size();
    }

    private void save(byte[] bytes) throws IOException
    {
        // TODO Why was this declared as a member variable and not locally?
        FileOutputStream output = null;
        try
        {
            Log.d(TAG, "saving image");

            String pic = IMAGE_FILE_FORMAT.format(Calendar.getInstance());

            File file = new File(Environment.getExternalStorageDirectory(), "DECO/EVENTS/");
            file.mkdirs(); //makes sure that the directory exists! if not then it makes it exist

            file = new File(Environment.getExternalStorageDirectory(), "DECO/EVENTS/" + pic + ".jpg");
            output = new FileOutputStream(file);
            output.write(bytes);
            output.flush();
            output.close();
            output = null;
            Log.d(TAG, "image written");

            Camera2BasicFragment.numEvents++;
        }
        catch (Exception e)
        {
            Log.e(TAG, "failed to save image", e);
        }
        finally
        {
            if (null != output)
            {
                output.close();
            }
        }
    }

    public void process()
    {
        if (!paths.isEmpty())
        {
            ready = false;

            Log.d(TAG, "taking image");
            byte[] bytes = paths.remove(0);

            Log.d(TAG, "decoding and scaling image");
            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;

            // TODO Why was bits declared as a member variable instead of a local variable?
            Bitmap origBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bitmapOptions);

            // TODO Why make a copy of the original image and scale it down? Why not just scale down the original image?
//          Bitmap bit = bits.copy(Bitmap.Config.ARGB_8888, true);
            // we want to scale the image down to something that will be usable and able to be processed alot faster
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(origBitmap, (int) (origBitmap.getWidth() / SCALE_X), (int) (origBitmap.getHeight() / SCALE_Y), false);
            Log.d(TAG, "scale (" + SCALE_X + "," + SCALE_Y + ") -> (" + scaledBitmap.getWidth() + "," + scaledBitmap.getHeight() + ")");
            Log.d(TAG, "image decoded and scaled");

            int height = scaledBitmap.getHeight();
            int width = scaledBitmap.getWidth();
            int redMax = 0;
            int greenMax = 0;
            int blueMax = 0;
            int numPixelsOverThreshold = 0;

            for (int x = 0; x < width; x++)
            {
                for (int y = 0; y < height; y++)
                {
                    int pixelColor = scaledBitmap.getPixel(x, y);
                    int red = (pixelColor & 0x00ff0000);
                    int green = (pixelColor & 0x0000ff00);
                    int blue = (pixelColor & 0x000000ff);

                    if (red > RED_THRESHOLD || blue > BLUE_THRESHOLD || green > GREEN_THRESHOLD)
                    {
                        numPixelsOverThreshold++;
                    }

                    if (red > redMax)
                        redMax = red;
                    if (green > greenMax)
                        greenMax = green;
                    if (blue > blueMax)
                        blueMax = blue;
                }
            }

            Log.d(TAG, "Max RGB values" + redMax + "," + greenMax + "," + blueMax);
            Log.d(TAG, "numPixelsOverThreshold " + numPixelsOverThreshold);

            if (numPixelsOverThreshold > 0 && numPixelsOverThreshold < 200)
            {
                longProcessor(origBitmap, bytes);
            }

            scaledBitmap.recycle();
            origBitmap.recycle();

            ready = true;
        }
    }

    public void longProcessor(Bitmap map, byte[] bits)
    {
        /*
        this will allow for full resolution proce3ssing of the images.
        should only be used for processing after the initial image filter has been used aka processor()
         */
        longProcess = true;

        int height = map.getHeight();
        int width = map.getWidth();
        int numPixelsOverThreshold = 0;

        for (int x = 0; x < width; x += 2)
        {
            for (int y = 0; y < height; y += 2)
            {
                int pixelColor = map.getPixel(x, y);
                int red = (pixelColor & 0x00ff0000);
                int green = (pixelColor & 0x0000ff00);
                int blue = (pixelColor & 0x000000ff);

                if (red > RED_THRESHOLD || blue > BLUE_THRESHOLD || green > GREEN_THRESHOLD)
                {
                    numPixelsOverThreshold++;
                }
            }
        }

        //Im paranoid this is comnpletely unneeded but it makes me feel better
        if (numPixelsOverThreshold > 3)
        {
            //it will catch any events that arnt caught cusing the return to break us.
            try
            {
                save(bits);
            }
            catch (Exception e)
            {
                Log.e(TAG, "failed to save image", e);
            }
        }

        longProcess = false;
    }

    public void run()
    {
        Log.d(TAG, "thread starting");
        while (true)
        {
            // TODO This is a tight loop without any sleep. It's likely consuming the CPU.
            while (go && ready)
            {
                process();

                if (paths.isEmpty())
                {
                    go = false;
                }

                // TODO ???
                if (paths.size() > 30)
                {
                    // TODO Rather than clear, should we just shrink instead?
                    paths.clear();
                    go = false;
                }
            }
        }
    }

}
