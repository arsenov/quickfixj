package quickfix.abfx;

import quickfix.FieldMap;
import quickfix.FieldNotFound;
import quickfix.Message;
import quickfix.field.*;
import quickfix.fix43.MarketDataSnapshotFullRefresh;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 *
 */
public class MarketDataSnaphotEncoder {

    public static String formatMarketDataMessage(Message msg) throws FieldNotFound {
        StringBuilder res = new StringBuilder(500);
        Message.Header header = msg.getHeader();
        res.append(header.getString(TargetCompID.FIELD)).append('#');
        res.append(header.getInt(MsgSeqNum.FIELD)).append(',');
        res.append(msg.getString(Symbol.FIELD)).append(',');
        int noMDEntries = msg.getInt(NoMDEntries.FIELD);
        if (noMDEntries == 2) {
            formatAdvertisedMDEntries(res, msg);
        } else if (noMDEntries == 0) {
            res.append("invalid");
        } else {
            throw new RuntimeException("unexpected number of entries in advertised market data: " + noMDEntries);
        }
        res.append(msg.getString(MDReqID.FIELD));
        return res.toString();
    }

    private static void formatAdvertisedMDEntries(StringBuilder res, Message msg) throws FieldNotFound {
        MarketDataSnapshotFullRefresh.NoMDEntries bid = new MarketDataSnapshotFullRefresh.NoMDEntries();
        MarketDataSnapshotFullRefresh.NoMDEntries ask = new MarketDataSnapshotFullRefresh.NoMDEntries();
        msg.getGroup(1, bid);
        msg.getGroup(2, ask);

        res.append(bid.getCurrency().getValue()).append(',');
        res.append(formatDate(bid.getMDEntryDate().getValue())).append(',');

        appendStringField(res, bid, FixCustomTags.FUT_SETT_DATE, ',');
        appendStringField(res, bid, FixCustomTags.TENOR_VALUE, ',');
        res.append(bid.getMDEntrySize().getValue()).append(',');

        appendStringField(res, bid, FixCustomTags.FUT_SETT_DATE2, ',');
        appendStringField(res, bid, FixCustomTags.TENOR_VALUE2, ',');
        appendDoubleField(res, bid, FixCustomTags.ORDER_QTY2, ',');

        res.deleteCharAt(res.length() -1).append('[');
        res.append(bid.getMDEntryPx().getValue()).append(',');
        appendDoubleField(res, bid, FixCustomTags.ORIG_BID_PX, ',');
        appendDoubleField(res, bid, FixCustomTags.BID_FORWARD_POINTS, ',');
        appendDoubleField(res, bid, FixCustomTags.BID_PX2, ',');
        appendDoubleField(res, bid, FixCustomTags.BID_FORWARD_POINTS2, ',');
        res.append(bid.getQuoteCondition().getValue());
        res.append('/');
        res.append(ask.getMDEntryPx().getValue()).append(',');
        appendDoubleField(res, ask, FixCustomTags.ORIG_OFFER_PX, ',');
        appendDoubleField(res, ask, FixCustomTags.OFFER_FORWARD_POINTS, ',');
        appendDoubleField(res, ask, FixCustomTags.OFFER_PX2, ',');
        appendDoubleField(res, ask, FixCustomTags.OFFER_FORWARD_POINTS2, ',');
        res.append(ask.getQuoteCondition().getValue());
        res.append(']');
    }

    private static final DateFormat valueDateFormat;

    static {
        valueDateFormat =  new SimpleDateFormat("yyyyMMdd", Locale.UK);
        valueDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    private static void appendStringField(StringBuilder res, FieldMap fieldMap, int field, char delim) throws FieldNotFound {
        if (fieldMap.isSetField(field)) res.append(fieldMap.getString(field)).append(delim);
    }

    private static void appendDoubleField(StringBuilder res, FieldMap fieldMap, int field, char delim) throws FieldNotFound {
        if (fieldMap.isSetField(field)) res.append(fieldMap.getDouble(field)).append(delim);
    }

    //todo use thread local here
    private static String formatDate(Date date) {
        synchronized(valueDateFormat) {
            return valueDateFormat.format(date);
        }
    }

}
