package org.savapage.lib.pgp.mime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.savapage.lib.pgp.PGPBaseException;
import org.savapage.lib.pgp.PGPHashAlgorithmEnum;
import org.savapage.lib.pgp.PGPHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PGPBodyPartSigner extends PGPBodyPartProcessor {

    /**
     *
     */
    private final PGPHashAlgorithmEnum hashAlgorithm =
            PGPHashAlgorithmEnum.SHA256;

    /**
     *
     * @param secretKey
     *            Secret key used for signing.
     * @param secretKeyPassword
     *            Passphrase for secret key.
     */
    public PGPBodyPartSigner(final PGPSecretKey secretKey,
            final String secretKeyPassword) {
        super(secretKey, secretKeyPassword);
    }

    /**
     *
     * @return The hash algorithm.
     */
    public PGPHashAlgorithmEnum getHashAlgorithm() {
        return hashAlgorithm;
    }

    /**
     *
     * @throws PGPMimeException
     *             When error.
     */
    public void sign() throws PGPMimeException {

        if (getContentPart() == null) {
            throw new PGPMimeException("content part is missing.");
        }

        InputStream contentStream = null;
        ByteArrayOutputStream contentStreamSigned = null;

        try {
            contentStream =
                    new ByteArrayInputStream(bodyPartAsString(getContentPart())
                            .getBytes(StandardCharsets.UTF_8));

            contentStreamSigned = new ByteArrayOutputStream();

            PGPHelper.instance().createSignature(contentStream,
                    contentStreamSigned, this.getSecretKey(),
                    this.getSecretKeyPassphrase(), this.hashAlgorithm);

            final BodyPart signedPart = new MimeBodyPart();
            signedPart.setContent(contentStreamSigned.toString(),
                    "application/pgp-signature");
            updateHeaders(signedPart);
            signedPart.setHeader("Content-Type",
                    "application/pgp-signature; " + "name=signature.asc");

            this.setProcessedPart(signedPart);

        } catch (IOException | MessagingException | PGPBaseException e) {
            throw new PGPMimeException(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(contentStreamSigned);
            IOUtils.closeQuietly(contentStream);
        }
    }

}
