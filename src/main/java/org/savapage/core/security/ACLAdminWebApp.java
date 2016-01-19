package org.savapage.core.security;

import java.util.EnumSet;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ACLAdminWebApp {

    public static final int BIT_1 = 0x1;
    public static final int BIT_2 = 0x2;
    public static final int BIT_3 = 0x4;
    public static final int BIT_4 = 0x8;
    public static final int BIT_5 = 0x10;
    public static final int BIT_6 = 0x20;
    public static final int BIT_7 = 0x40;
    public static final int BIT_8 = 0x80;

    /**
     * .
     */
    public static enum Item {
        // ------------------------------
        DASHBOARD_VIEW(BIT_1),

        // ------------------------------

        // "Change user settings".
        USERS_EDIT(BIT_1),
        //
        USERS_CREATE_INTERNAL(BIT_2),
        //
        USERS_CHANGE_PASSWORD(BIT_3),
        //
        USERS_ADJUST_BALANCE(BIT_4)
        //
        ;

        final int bitmask;

        /**
         * .
         */
        private Item(final int mask) {
            this.bitmask = mask;
        }

    }

    /**
     * .
     */
    public static enum Section {

        /**
         * .
         */
        DASHBOARD(EnumSet.of(Item.DASHBOARD_VIEW)),

        /**
         * .
         */
        USERS(EnumSet.of(Item.USERS_EDIT, Item.USERS_CHANGE_PASSWORD,
                Item.USERS_CREATE_INTERNAL, Item.USERS_ADJUST_BALANCE));

        /**
         *
         */
        private final EnumSet<Item> items;

        /**
         *
         * @param sub
         */
        private Section(final EnumSet<Item> sub) {
            this.items = sub;
        }

        public EnumSet<Item> getItems() {
            return this.items;
        }
    }

}
