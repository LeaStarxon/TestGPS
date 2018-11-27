package leastarxon.dev.testgps.Utils;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;


public class PermissionHelper {
	private static final String PERM_AFL = "android.permission.ACCESS_FINE_LOCATION";
	private static final String PERM_ACL = "android.permission.ACCESS_COARSE_LOCATION";


	public static boolean checkPermissionsForGPS(AppCompatActivity context) {
		return checkPermissions(context, PERM_ACL, PERM_AFL);
	}

	private static boolean checkPermissions(Activity context, String... params) {
		boolean answer = true;
		ArrayList<String> nonGranted = new ArrayList<>();
		for (String s : params) {
			int res = context.checkCallingOrSelfPermission(s);
			if (res == PackageManager.PERMISSION_DENIED) {
				nonGranted.add(s);
			}
			answer &= (res == PackageManager.PERMISSION_GRANTED);
		}
		if (!answer) {
			int sdkInt = Build.VERSION.SDK_INT;
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
				context.requestPermissions(
						nonGranted.toArray(new String[nonGranted.size()]), 1
				);
			}
		}
		return answer;
	}
}
