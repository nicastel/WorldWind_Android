/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.cache.Cacheable;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWIO;
import gov.nasa.worldwind.util.WWUtil;
import gov.nasa.worldwind.util.dds.DDSTextureReader;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.opengl.GLES20;
import android.opengl.GLUtils;


/**
 * @author dcollins
 * @version $Id: GpuTextureData.java 764 2012-09-11 00:17:17Z tgaskins $
 */
public class GpuTextureData implements Cacheable
{
    public static class BitmapData
    {
        public final Bitmap bitmap;

        public BitmapData(Bitmap bitmap)
        {
            if (bitmap == null)
            {
                String msg = Logging.getMessage("nullValue.BitmapIsNull");
                Logging.error(msg);
                throw new IllegalArgumentException(msg);
            }

            this.bitmap = bitmap;
        }
    }

    public static class CompressedData
    {
        public final int format;
        public final MipmapData[] levelData;

        public CompressedData(int format, MipmapData[] levelData)
        {
            if (levelData == null || levelData.length == 0)
            {
                String msg = Logging.getMessage("nullValue."); // TODO
                Logging.error(msg);
                throw new IllegalArgumentException(msg);
            }

            this.format = format;
            this.levelData = levelData;
        }
    }

    public static class MipmapData
    {
        public final int width;
        public final int height;
        public final ByteBuffer buffer;

        public MipmapData(int width, int height, ByteBuffer buffer)
        {
            if (width < 0)
            {
                String msg = Logging.getMessage("generic.WidthIsInvalid", width);
                Logging.error(msg);
                throw new IllegalArgumentException(msg);
            }

            if (height < 0)
            {
                String msg = Logging.getMessage("generic.HeightIsInvalid", height);
                Logging.error(msg);
                throw new IllegalArgumentException(msg);
            }

            if (buffer == null)
            {
                String msg = Logging.getMessage("nullValue.BufferIsNull");
                Logging.error(msg);
                throw new IllegalArgumentException(msg);
            }

            this.width = width;
            this.height = height;
            this.buffer = buffer;
        }
    }

    public static GpuTextureData createTextureData(Object source)
    {
        if (WWUtil.isEmpty(source))
        {
            String msg = Logging.getMessage("nullValue.SourceIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        GpuTextureData data = null;

        try
        {
            if (source instanceof Bitmap)
            {
                data = new GpuTextureData((Bitmap) source, estimateMemorySize((Bitmap) source));
            }
            else
            {
                // Attempt to open the source as an InputStream. This handle URLs, Files, InputStreams, a String
                // containing a valid URL, a String path to a file on the local file system, and a String path to a
                // class path resource.
                InputStream stream = WWIO.openStream(source);
                try
                {
                    if (stream != null)
                    {
                        // Wrap the stream in a BufferedInputStream to provide the mark/reset capability required to
                        // avoid destroying the stream when it is read more than once. BufferedInputStream also improves
                        // file read performance.
                        if (!(stream instanceof BufferedInputStream))
                            stream = new BufferedInputStream(stream);
                        data = fromStream(stream);
                    }
                }
                finally
                {
                    WWIO.closeStream(stream, source.toString()); // This method call is benign if the stream is null.
                }
            }
        }
        catch (Exception e)
        {
            String msg = Logging.getMessage("GpuTextureFactory.TextureDataCreationFailed", source);
            Logging.error(msg);
        }

        return data;
    }

    protected static final int DEFAULT_MARK_LIMIT = 1024;
    
//    public static RenderScript rs;
//    public static ScriptC_etc1compressor script;

    protected static GpuTextureData fromStream(InputStream stream)
    {
        GpuTextureData data = null;
        try
        {
            stream.mark(DEFAULT_MARK_LIMIT);

            DDSTextureReader ddsReader = new DDSTextureReader();
            data = ddsReader.read(stream);
            if (data != null)
                return data;

            stream.reset();            

    		Options opts = new BitmapFactory.Options();
    		opts.inPreferQualityOverSpeed = false;
    		opts.inPreferredConfig = Config.RGB_565;
            Bitmap bitmap = BitmapFactory.decodeStream(stream, null, opts);
//            
//            if(bitmap != null) {
//            	ETC1Texture texture = RsETC1Util.compressBitmap(rs, script, bitmap);
//            	if (texture != null) {
//    				int estimatedMemorySize = ETC1.ETC_PKM_HEADER_SIZE
//    						+ texture.getHeight() * texture.getWidth() / 2;
//    				return PKMGpuTextureData.fromPKMETC1CompressedData(texture,
//    						estimatedMemorySize);
//    			} else {
//    				return null;
//    			}
//            }
            
            return bitmap != null ? new GpuTextureData(bitmap, estimateMemorySize(bitmap)) : null;
        }
        catch (IOException e)
        {
            // TODO
        }

        return data;
    }

    public GpuTextureData(Bitmap bitmap, long estimatedMemorySize)
    {
        if (bitmap == null)
        {
            String msg = Logging.getMessage("nullValue.BitmapIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (estimatedMemorySize <= 0)
        {
            String msg = Logging.getMessage("generic.SizeIsInvalid", estimatedMemorySize);
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.bitmapData = new BitmapData(bitmap);
        this.estimatedMemorySize = estimatedMemorySize;
    }

    public GpuTextureData(int format, MipmapData[] levelData, long estimatedMemorySize)
    {
        if (levelData == null || levelData.length == 0)
        {
            String msg = Logging.getMessage("nullValue."); // TODO
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (estimatedMemorySize <= 0)
        {
            String msg = Logging.getMessage("generic.SizeIsInvalid", estimatedMemorySize);
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.compressedData = new CompressedData(format, levelData);
        this.estimatedMemorySize = estimatedMemorySize;
    }

    protected BitmapData bitmapData;
    protected CompressedData compressedData;
    protected long estimatedMemorySize;

    protected GpuTextureData()
    {
    }

    public BitmapData getBitmapData()
    {
        return this.bitmapData;
    }

    public CompressedData getCompressedData()
    {
        return this.compressedData;
    }

    public long getSizeInBytes()
    {
        return this.estimatedMemorySize;
    }

    protected static long estimateMemorySize(Bitmap bitmap)
    {
        int internalFormat = GLUtils.getInternalFormat(bitmap);

        if (internalFormat == GLES20.GL_ALPHA || internalFormat == GLES20.GL_LUMINANCE)
        {
            // Alpha and luminance pixel data is always stored as 1 byte per pixel. See OpenGL ES Specification, version 2.0.25,
            // section 3.6.2, table 3.4.
            return bitmap.getWidth() * bitmap.getHeight();
        }
        else if (internalFormat == GLES20.GL_LUMINANCE_ALPHA)
        {
            // Luminance-alpha pixel data is always stored as 2 bytes per pixel. See OpenGL ES Specification,
            // version 2.0.25, section 3.6.2, table 3.4.
            return 2 * bitmap.getWidth() * bitmap.getHeight(); // Type must be GL_UNSIGNED_BYTE.
        }
        else if (internalFormat == GLES20.GL_RGB)
        {
            // RGB pixel data is stored as either 2 or 3 bytes per pixel, depending on the type used during texture
            // image specification. See OpenGL ES Specification, version 2.0.25, section 3.6.2, table 3.4.
            int type = GLUtils.getType(bitmap);
            // Default to type GL_UNSIGNED_BYTE.
            int bpp = (type == GLES20.GL_UNSIGNED_SHORT_5_6_5 ? 2 : 3);
            return bpp * bitmap.getWidth() * bitmap.getHeight();
        }
        else // Default to internal format GL_RGBA.
        {
            // RGBA pixel data is stored as either 2 or 4 bytes per pixel, depending on the type used during texture
            // image specification. See OpenGL ES Specification, version 2.0.25, section 3.6.2, table 3.4.
            int type = GLUtils.getType(bitmap);
            // Default to type GL_UNSIGNED_BYTE.
            int bpp = (type == GLES20.GL_UNSIGNED_SHORT_4_4_4_4 || type == GLES20.GL_UNSIGNED_SHORT_5_5_5_1) ? 2 : 4;
            return bpp * bitmap.getWidth() * bitmap.getHeight();
        }
    }
}
