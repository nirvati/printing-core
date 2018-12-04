package org.savapage.lib.pgp.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

import org.savapage.lib.pgp.PGPBaseException;

/**
 * Reader of a PDF or PDF with embedded PGP signature.
 *
 * @author Rijk Ravestein
 *
 */
public abstract class PdfPgpReader {

    /** */
    public static final int INT_PDF_COMMENT = '%';

    /** */
    public static final String PDF_COMMENT_PFX = "%";

    /** */
    public static final String PDF_COMMENT_BEGIN_PGP_SIGNATURE =
            PDF_COMMENT_PFX + "-----BEGIN PGP SIGNATURE-----";

    /** */
    public static final String PDF_COMMENT_END_PGP_SIGNATURE =
            PDF_COMMENT_PFX + "-----END PGP SIGNATURE-----";

    /** */
    private static final String PDF_EOF = "%%EOF";

    /** */
    protected static final int INT_NEWLINE = '\n';

    /**
     * @return A message digest, or {@code null} when not applicable.
     */
    protected abstract MessageDigest createMessageDigest();

    /**
     *
     * @param sig
     *            The ASCII armored PGP signature.
     */
    protected abstract void onPgpSignature(byte[] sig);

    /**
     *
     * @param content
     *            PDF content.
     * @throws IOException
     *             If IO error.
     */
    protected abstract void onPdfContent(byte[] content) throws IOException;

    /**
     *
     * @param content
     *            PDF content.
     * @throws IOException
     *             If IO error.
     */
    protected abstract void onPdfContent(byte content) throws IOException;

    /**
     * @throws IOException
     *             If IO error.
     */
    protected abstract void onPdfEof() throws IOException;

    /**
     *
     * @throws IOException
     *             If IO error.
     */
    protected abstract void onStart() throws IOException;

    /**
     *
     * @param messageDigest
     *            Message digest of PDF content.
     * @param byteCount
     *            Number of PDF content bytes digested.
     * @throws IOException
     *             If IO error.
     */
    protected abstract void onEnd(MessageDigest messageDigest, long byteCount)
            throws IOException;

    /**
     * State of reader.
     */
    private enum ReadState {
        /**
         * Collecting PDF content.
         */
        COLLECT_PDF_CONTENT,
        /**
         * Collecting PGP signature.
         */
        COLLECT_PDF_SIGNATURE,
        /**
         * Collecting EOF.
         */
        COLLECT_PDF_EOF,
        /**
         * Stop collecting.
         */
        COLLECT_END
    }

    /**
     * Message digest of PDF content.
     */

    private MessageDigest contentMessageDigest;

    /**
     * Number of PDF content bytes digested.
     */
    private long contentBytes;

    /**
     *
     * @param content
     *            bytes.
     */
    private void digestPdfContent(final byte[] content) {
        this.contentBytes += content.length;
        if (this.contentMessageDigest != null) {
            this.contentMessageDigest.update(content);
        }
    }

    /**
     *
     * @param content
     *            byte.
     */
    private void digestPdfContent(final byte content) {
        this.contentBytes++;
        if (this.contentMessageDigest != null) {
            this.contentMessageDigest.update(content);
        }
    }

    /**
     *
     * @param istrPdf
     *            The PDF input stream.
     * @throws PGPBaseException
     *             If PGP error.
     */
    public void read(final InputStream istrPdf) throws PGPBaseException {

        this.contentBytes = 0;
        this.contentMessageDigest = createMessageDigest();

        try (ByteArrayOutputStream bosSignature =
                new ByteArrayOutputStream();) {

            onStart();

            int n = istrPdf.read();

            boolean isNewLine = true;

            ReadState readState = ReadState.COLLECT_PDF_CONTENT;

            boolean collectedPdfEof = false;
            boolean collectedPgpSig = false;

            while (n > -1) {

                if (isNewLine && n == INT_PDF_COMMENT) {

                    /*
                     * Collect raw bytes! Do not collect on String, because
                     * bytes will be interpreted as Unicode.
                     */
                    final ByteArrayOutputStream bosAhead =
                            new ByteArrayOutputStream();

                    // Read till EOL or EOF.
                    isNewLine = false;

                    while (n > -1) {
                        bosAhead.write(n);
                        if (isNewLine) {
                            break;
                        }
                        // Read next
                        n = istrPdf.read();
                        isNewLine = n == INT_NEWLINE;
                    }

                    final byte[] bytesAhead = bosAhead.toByteArray();

                    /*
                     * At this point we do convert to String, so we can compare.
                     */
                    final String stringAhead = new String(bytesAhead);
                    bosAhead.close();

                    if (stringAhead
                            .startsWith(PDF_COMMENT_BEGIN_PGP_SIGNATURE)) {
                        // Skip first % character.
                        bosSignature.write(stringAhead.substring(1).getBytes());
                        readState = ReadState.COLLECT_PDF_SIGNATURE;
                        continue;
                    }

                    if (stringAhead.startsWith(PDF_COMMENT_END_PGP_SIGNATURE)) {
                        // Skip first % character.
                        bosSignature.write(stringAhead.substring(1).getBytes());
                        readState = ReadState.COLLECT_PDF_EOF;
                        collectedPgpSig = true;
                        if (collectedPdfEof) {
                            readState = ReadState.COLLECT_END;
                        }
                        continue;
                    }

                    if (readState == ReadState.COLLECT_PDF_SIGNATURE) {
                        // Skip first % character.
                        bosSignature.write(stringAhead.substring(1).getBytes());
                        continue;
                    }

                    if (stringAhead.startsWith(PDF_EOF)) {
                        onPdfEof();
                        collectedPdfEof = true;
                    }

                    if (readState != ReadState.COLLECT_END) {
                        onPdfContent(bytesAhead);
                        digestPdfContent(bytesAhead);
                    }

                    if (collectedPdfEof && collectedPgpSig) {
                        readState = ReadState.COLLECT_END;
                    }

                } else {
                    isNewLine = n == INT_NEWLINE;
                    if (readState == ReadState.COLLECT_PDF_CONTENT) {
                        onPdfContent((byte) n);
                        digestPdfContent((byte) n);
                    }
                }

                // Read next when not EOF.
                if (n > -1) {
                    n = istrPdf.read();
                }
            }

            /*
             * Collect PGP signature.
             */
            if (bosSignature.size() > 0) {

                bosSignature.flush();
                bosSignature.close();

                final byte[] pgpBytes = bosSignature.toByteArray();
                onPgpSignature(pgpBytes);
            }

            onEnd(this.contentMessageDigest, this.contentBytes);

        } catch (IOException e) {
            throw new PGPBaseException(e.getMessage(), e);
        }
    }

}
