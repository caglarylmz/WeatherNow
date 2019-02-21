package oriontech.com.weathernow.utils;

import android.content.res.Resources;

public class Common {

    public static double locationLat;
    public static double locationLon;
    public static String locationLastUpdateTime;

    public static int getStatusBarHeight() {
        final Resources res = Resources.getSystem();
        int id = Resources.getSystem().getIdentifier("status_bar_height", "dimen", "android");
        if (id > 0) {
            return res.getDimensionPixelSize(id);
        }
        return 0;
    }


}
