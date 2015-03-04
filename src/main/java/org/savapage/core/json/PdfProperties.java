/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.json;

import org.savapage.core.crypto.CryptoUser;

/**
 * The PDF properties as saved per user.
 *
 * @author Datraverse B.V.
 *
 */
public class PdfProperties extends JsonAbstractBase {

    public static class PdfAllow {

        private Boolean printing = false;
        private Boolean degradedPrinting = false;
        private Boolean modifyContents = false;
        private Boolean modifyAnnotations = false;
        private Boolean assembly = false;
        private Boolean copy = false;
        private Boolean copyForAccess = false;
        private Boolean fillin = false;

        public Boolean getPrinting() {
            return printing;
        }

        public void setPrinting(Boolean printing) {
            this.printing = printing;
        }

        public Boolean getDegradedPrinting() {
            return degradedPrinting;
        }

        public void setDegradedPrinting(Boolean degradedPrinting) {
            this.degradedPrinting = degradedPrinting;
        }

        public Boolean getModifyContents() {
            return modifyContents;
        }

        public void setModifyContents(Boolean modifyContents) {
            this.modifyContents = modifyContents;
        }

        public Boolean getModifyAnnotations() {
            return modifyAnnotations;
        }

        public void setModifyAnnotations(Boolean modifyAnnotations) {
            this.modifyAnnotations = modifyAnnotations;
        }

        public Boolean getAssembly() {
            return assembly;
        }

        public void setAssembly(Boolean assembly) {
            this.assembly = assembly;
        }

        public Boolean getCopy() {
            return copy;
        }

        public void setCopy(Boolean copy) {
            this.copy = copy;
        }

        public Boolean getCopyForAccess() {
            return copyForAccess;
        }

        public void setCopyForAccess(Boolean copyForAccess) {
            this.copyForAccess = copyForAccess;
        }

        public Boolean getFillin() {
            return fillin;
        }

        public void setFillin(Boolean fillin) {
            this.fillin = fillin;
        }
    }

    public static class Apply {

        private Boolean subject = true;
        private Boolean keywords = true;
        private Boolean encryption = true;
        private Boolean passwords = true;

        public Boolean getSubject() {
            return subject;
        }

        public void setSubject(Boolean subject) {
            this.subject = subject;
        }

        public Boolean getKeywords() {
            return keywords;
        }

        public void setKeywords(Boolean keywords) {
            this.keywords = keywords;
        }

        public Boolean getEncryption() {
            return encryption;
        }

        public void setEncryption(Boolean encryption) {
            this.encryption = encryption;
        }

        public Boolean getPasswords() {
            return passwords;
        }

        public void setPasswords(Boolean passwords) {
            this.passwords = passwords;
        }
    }

    public static class PdfDesc {

        private String title = "";
        private String author = "";
        private String subject = "";
        private String keywords = "";

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getKeywords() {
            return keywords;
        }

        public void setKeywords(String keywords) {
            this.keywords = keywords;
        }
    }

    public static class PdfPasswords {

        private String owner = "";
        private String user = "";

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        /**
         * Encrypts the passwords so they can be stored.
         */
        public void encrypt() {
            if (!owner.isEmpty()) {
                owner = encrypt(owner);
            }
            if (!user.isEmpty()) {
                user = encrypt(user);
            }
        }

        /**
         * Decrypts the passwords so they can be applied.
         */
        public void decrypt() {
            if (!owner.isEmpty()) {
                owner = decrypt(owner);
            }
            if (!user.isEmpty()) {
                user = decrypt(user);
            }
        }

        public static String decrypt(final String str) {
            return CryptoUser.decrypt(str);
        }

        public static String encrypt(final String pw) {
            return CryptoUser.encrypt(pw);
        }
    }

    private String encryption = ""; // must NOT be null
    private PdfDesc desc = new PdfDesc();
    private PdfAllow allow = new PdfAllow();
    private PdfPasswords pw = new PdfPasswords();
    private Apply apply = new Apply();

    public String getEncryption() {
        return encryption;
    }

    public void setEncryption(String s) {
        encryption = s;
    }

    public PdfDesc getDesc() {
        return desc;
    }

    public void setDesc(PdfDesc desc) {
        this.desc = desc;
    }

    public PdfAllow getAllow() {
        return allow;
    }

    public void setAllow(PdfAllow allow) {
        this.allow = allow;
    }

    public PdfPasswords getPw() {
        return pw;
    }

    public void setPw(PdfPasswords pw) {
        this.pw = pw;
    }

    public Apply getApply() {
        return apply;
    }

    public void setApply(Apply apply) {
        this.apply = apply;
    }

    /**
     * Creates an instance to be used as default.
     *
     * <p>
     * Note: attributes of this class and contained classes have a proper
     * default value (not null) so the created object can be used as default in
     * the WebApp.
     * </p>
     */
    public static PdfProperties createDefault() {
        return new PdfProperties();
    }

    /**
     * Creates an instance from JSON string.
     *
     * @param json
     * @return
     * @throws Exception
     */
    public static PdfProperties create(final String json) throws Exception {
        return getMapper().readValue(json, PdfProperties.class);
    }

}
