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
package org.savapage.core.services.impl;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.savapage.core.dao.AccountVoucherDao;
import org.savapage.core.dto.AccountVoucherBatchDto;
import org.savapage.core.jpa.AccountVoucher;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodResponse;
import org.savapage.core.json.rpc.JsonRpcMethodResult;
import org.savapage.core.services.AccountVoucherService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.AccountVoucherUtil;
import org.savapage.core.util.BigDecimalUtil;

import com.ibm.icu.util.Calendar;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class AccountVoucherServiceImpl extends AbstractService implements
        AccountVoucherService {

    @Override
    public boolean isVoucherExpired(final AccountVoucher voucher,
            final Date dateToday) {

        return DateUtils.truncate(voucher.getExpiryDate(),
                Calendar.DAY_OF_MONTH).getTime() <= DateUtils.truncate(
                dateToday, Calendar.DAY_OF_MONTH).getTime();
    }

    @Override
    public AbstractJsonRpcMethodResponse
            createBatch(AccountVoucherBatchDto dto) {

        /*
         * VALIDATE batch.
         */
        if (StringUtils.isBlank(dto.getBatchId())) {
            return createError("msg-voucher-batch-invalid", dto.getBatchId());
        }

        /*
         * INVARIANT: Batch-ID must be unique.
         */
        if (accountVoucherDAO().countVouchersInBatch(dto.getBatchId()) > 0) {
            return createError("msg-voucher-batch-exist", dto.getBatchId());
        }

        /*
         * VALIDATE number.
         */
        if (dto.getNumber() == null) {
            return createError("msg-voucher-batch-number-invalid", "");
        }

        /*
         * VALIDATE value.
         */
        if (dto.getValue() == null) {
            return createError("msg-voucher-value-invalid", "");
        }

        BigDecimal valueAmount;
        try {
            valueAmount =
                    BigDecimalUtil.parse(dto.getValue(),
                            ServiceContext.getLocale(), false, false);
        } catch (ParseException e) {
            return createError("msg-voucher-value-invalid", dto.getValue());
        }

        /*
         * VALIDATE expiry.
         */
        if (dto.getExpiryDate() == null) {
            return createError("msg-voucher-expiry-invalid", "");
        }

        /*
         * INVARIANT: expiry cannot be in the past.
         */
        final Date now = new Date();
        final Date expiry = dto.dayOfMonth(dto.getExpiryDate());

        if (expiry.getTime() < now.getTime()) {
            return createError("msg-voucher-expiry-in-past");
        }

        /*
         * Create the cards
         */
        final Set<String> voucherNumbers =
                AccountVoucherUtil.generateVoucherNumbers(dto.getBatchId(), dto
                        .getNumber().intValue());

        final Iterator<String> iter = voucherNumbers.iterator();

        while (iter.hasNext()) {

            String cardNumber = iter.next();

            AccountVoucher voucher = new AccountVoucher();
            voucher.setCardNumber(cardNumber);
            voucher.setCardNumberBatch(dto.getBatchId());
            voucher.setCreatedDate(now);
            voucher.setExpiryDate(expiry);
            voucher.setIssuedDate(now);
            voucher.setUuid(UUID.randomUUID().toString());
            voucher.setValueAmount(valueAmount);
            voucher.setVoucherType(AccountVoucherDao.DbVoucherType.CARD
                    .toString());

            accountVoucherDAO().create(voucher);
        }

        //
        return JsonRpcMethodResult.createOkResult();
    }

}
