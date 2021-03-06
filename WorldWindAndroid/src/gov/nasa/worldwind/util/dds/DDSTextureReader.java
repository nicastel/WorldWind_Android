/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.util.dds;

import gov.nasa.worldwind.render.GpuTextureData;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWIO;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * @author dcollins
 * @version $Id: DDSTextureReader.java 763 2012-09-07 22:07:32Z tgaskins $
 */
public class DDSTextureReader
{
    // DXT compression internal formats. See http://www.opengl.org/registry/specs/EXT/texture_compression_s3tc.txt.
    protected static final int GL_COMPRESSED_RGB_S3TC_DXT1_EXT = 0x83F0;
    protected static final int GL_COMPRESSED_RGBA_S3TC_DXT1_EXT = 0x83F1;
    protected static final int GL_COMPRESSED_RGBA_S3TC_DXT3_EXT = 0x83F2;
    protected static final int GL_COMPRESSED_RGBA_S3TC_DXT5_EXT = 0x83F3;
    
    // ETC1 compression internal formats. See http://www.khronos.org/registry/gles/extensions/OES/OES_compressed_ETC1_RGB8_texture.txt.
    protected static final int GL_ETC1_RGB8_OES           		= 0x8D64;

    public DDSTextureReader()
    {
    }

    public GpuTextureData read(InputStream stream)
    {
        if (stream == null)
        {
            String msg = Logging.getMessage("nullValue.InputStreamIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        GpuTextureData data = null;
        try
        {
            data = this.doRead(stream);
        }
        catch (Exception e)
        {
            // Intentionally left blank. DDSTextureReader just silently returns null for now.
        }

        return data;
    }

    protected GpuTextureData doRead(InputStream stream) throws IOException
    {
        DDSHeader header = DDSHeader.read(stream);

        int width = header.getWidth();
        if (width < 1)
        {
            String msg = Logging.getMessage("generic.WidthIsInvalid", width);
            throw new IllegalArgumentException(msg);
        }

        int height = header.getHeight();
        if (height < 1)
        {
            String msg = Logging.getMessage("generic.HeightIsInvalid", height);
            throw new IllegalArgumentException(msg);
        }

        int format = this.getFormat(header);
        if (format == 0)
        {
            String msg = Logging.getMessage("generic.FormatIsInvalid", format);
            throw new IllegalArgumentException(msg);
        }

        int mipmapCount = Math.max(1,header.getMipMapCount());
        long estimatedMemorySize = 0;

        ByteBuffer buffer = WWIO.readStreamToBuffer(stream);
        GpuTextureData.MipmapData[] levelData = new GpuTextureData.MipmapData[mipmapCount];

        for (int i = 0; i < mipmapCount; i++)
        {
            int size = this.getImageSize(header, width, height);
            int limit = buffer.position() + size;
            buffer.limit(limit);

            levelData[i] = new GpuTextureData.MipmapData(width, height, buffer.slice());
            estimatedMemorySize += size;

            buffer.limit(buffer.capacity());
            buffer.position(limit);

            width = Math.max(width / 2, 1);
            height = Math.max(height / 2, 1);
        }

        return new GpuTextureData(format, levelData, estimatedMemorySize);
    }

    protected int getFormat(DDSHeader header)
    {
        if (header.getPixelFormat() == null)
            return 0;

        int fourcc = header.getPixelFormat().getFourCC();

        if (fourcc == DDSConstants.D3DFMT_DXT1)
            return GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;

        else if (fourcc == DDSConstants.D3DFMT_DXT2 || fourcc == DDSConstants.D3DFMT_DXT3)
            return GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;

        else if (fourcc == DDSConstants.D3DFMT_DXT4 || fourcc == DDSConstants.D3DFMT_DXT5)
            return GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
        
        else if (fourcc == DDSConstants.D3DFMT_ETC1)
            return GL_ETC1_RGB8_OES;

        return 0;
    }

    protected int getImageSize(DDSHeader header, int width, int height)
    {
        if (header.getPixelFormat() == null)
            return 0;

        int minWidth = Math.max(width, 4);
        int minHeight = Math.max(height, 4);

        int fourcc = header.getPixelFormat().getFourCC();

        if (fourcc == DDSConstants.D3DFMT_DXT1 || fourcc == DDSConstants.D3DFMT_ETC1)
            return (minWidth * minHeight) / 2;

        else if (fourcc == DDSConstants.D3DFMT_DXT2 || fourcc == DDSConstants.D3DFMT_DXT3)
            return minWidth * minHeight;

        else if (fourcc == DDSConstants.D3DFMT_DXT4 || fourcc == DDSConstants.D3DFMT_DXT5)
            return minWidth * minHeight;

        return 0;
    }
}
