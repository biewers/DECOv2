package edu.wisc.physics.icecube.decov2;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.test.AndroidTestCase;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * Created by andrewbiewer on 3/25/15.
 */
public class DataProcessorTest extends AndroidTestCase
{
    public DataProcessorTest()
    {
    }

    public void testProcess()
    {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap image = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.eightpixels2, options);
        assertNotNull(image);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] bytes = baos.toByteArray();

        DataProcessor p = new DataProcessor();
        p.setImage(bytes);
        p.process();
    }
}
