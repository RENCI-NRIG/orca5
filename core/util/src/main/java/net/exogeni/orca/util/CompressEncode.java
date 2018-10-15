package net.exogeni.orca.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.apache.commons.codec.binary.Base64;

/**
 * This is a simple static compressor/decompressor that takes a string,
 * compresses and base64-encodes it (or does the reverse)
 * @author ibaldin
 *
 */
public class CompressEncode {

	
	/**
	 * Compress (gzip) and base64 encode the string
	 * @param inputString encoded string to be compressed
	 * @return compressed encoded string
	 */
	public static String compressEncode(String inputString) {
		// compress
		byte[] input = inputString.getBytes();
		ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);

		Deflater comp = new Deflater();
		comp.setLevel(Deflater.BEST_SPEED);
		comp.setInput(input);
		comp.finish();

		byte[] tmp = new byte[1024];
		while(!comp.finished()) {
			int count = comp.deflate(tmp);
			bos.write(tmp, 0, count);
		}

		try {
			bos.close();
		} catch (IOException e) {
			;
		}
		// base64-encode
		byte[] resBytes = Base64.encodeBase64(bos.toByteArray());

		String res = new String(resBytes);

		return res;
	}
	
	public static String decodeDecompress(String inputString) throws DataFormatException {
		
		// base 64 decode
		byte[] decoded = Base64.decodeBase64(inputString.getBytes());

		// decompress
		Inflater decompressor = new Inflater();
		decompressor.setInput(decoded);

		// Create an expandable byte array to hold the decompressed data
		ByteArrayOutputStream bos = new ByteArrayOutputStream(decoded.length);

		try {
			// Decompress the data
			byte[] buf = new byte[1024];
			while (!decompressor.finished()) {
				int count = decompressor.inflate(buf);
				bos.write(buf, 0, count);
			}
		} finally {
			try {
				bos.close();
			} catch (IOException e) {
				;
			}
		}

		// Get the decompressed data
		byte[] decompressedData = bos.toByteArray();

		return new String(decompressedData);
	}
}
