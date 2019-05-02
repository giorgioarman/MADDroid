package it.polito.maddroid.lab3.common;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Parcelable;
import android.provider.MediaStore;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.content.FileProvider;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;


public class Utility {
    private static final String TAG = "Utility";
    private static final ArrayList<String> days = new ArrayList<>(Arrays.asList("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"));

    public static void showAlertToUser(Activity activity, int stringResId) {
        String alert = activity.getResources().getString(stringResId);
        Snackbar.make(activity.findViewById(android.R.id.content),alert,Snackbar.LENGTH_SHORT).show();
    }
    
    public static void startActivityToGetImage(Activity context, String AUTHORITY, File destFile, int requestCode) {
        File myImageFile = destFile;
        
        final Uri outputFileUri = FileProvider.getUriForFile(context.getApplicationContext(),
                AUTHORITY,
                myImageFile);
        
        
        Log.d(TAG, "Uri: " + outputFileUri.toString());
        
        // Camera
        final List<Intent> totIntents = new ArrayList<>();
        final Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        final PackageManager packageManager = context.getPackageManager();
        final List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for(ResolveInfo res : listCam) {
            final String packageName = res.activityInfo.packageName;
            final Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(packageName, res.activityInfo.name));
            intent.setPackage(packageName);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            totIntents.add(intent);
        }
        
        // Filesystem
        final Intent filePickIntent = new Intent(Intent.ACTION_GET_CONTENT);
        filePickIntent.setType("image/*");
        final List<ResolveInfo> listGalleries = packageManager.queryIntentActivities(filePickIntent, 0);
        for(ResolveInfo res : listGalleries) {
            final String packageName = res.activityInfo.packageName;
            final Intent intent = new Intent(filePickIntent);
            intent.setComponent(new ComponentName(packageName, res.activityInfo.name));
            intent.setPackage(packageName);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            totIntents.add(intent);
        }
        
        Intent lastIntent = totIntents.remove(totIntents.size() - 1);
        
        // Chooser of filesystem options.
        final Intent chooserIntent = Intent.createChooser(lastIntent, "Select Source");
        
        // Add the camera options.
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, totIntents.toArray(new Parcelable[totIntents.size()]));
        
        context.startActivityForResult(chooserIntent, requestCode);
    }

    public static String extractTimeTable(String s){
        for ( int i = 0; i < 7 ; ++i)
            s = s.replace("Day"+i, days.get(i));
        s = s.replace("_", " ");
        s = s.replace(",", "    ");
        s = s.replace(";", "\n");

        System.out.println(s);
        return s;
    }

    public static boolean openRestaurant ( String timeTable, int day, int hour, int minutes)
    {
        int timeHourP;
        int timeMinutesP;
        int timeHourA;
        int timeMinutesA;
        boolean [] restaurantWeeklyOpen = new boolean[10080];

        if ((hour < 0 || hour > 23 )&& (minutes < 0 || minutes > 59) && (day < 0 || day > 6) && timeTable.isEmpty())
            return false;

        String [] allWeek = timeTable.split(";");
        if (allWeek[day].contains("closed"))
            return false;

        for (int indexDay = 0; indexDay < 7 ; ++indexDay)
        {
            if (allWeek[indexDay].contains("closed"))
                continue;
            String [] allDay = allWeek[indexDay].split(",");
            for (int j = 1; j < allDay.length; ++j)
            {
                String [] timeFirst = allDay[j].split("_");
                timeFirst[0] = timeFirst[0].replace(":", " ");
                timeFirst[1] = timeFirst[1].replace(":", " ");
                timeHourP = Integer.parseInt(timeFirst[0].substring(0, 2));
                timeMinutesP = Integer.parseInt(timeFirst[0].substring(3));
                timeMinutesP = (timeHourP*60)+ timeMinutesP + (1440* indexDay);

                timeHourA = Integer.parseInt(timeFirst[1].substring(0, 2));
                timeMinutesA = Integer.parseInt(timeFirst[1].substring(3));
                if(timeHourP > timeHourA)
                    indexDay ++;
                timeMinutesA = (timeHourA*60)+ timeMinutesA + (1440* indexDay);


                for (int count = timeMinutesP ; count < timeMinutesA ; ++count)
                    restaurantWeeklyOpen[count] = true;

            }
        }

    return (restaurantWeeklyOpen[minutes + (hour*60) + (1440 *day)]);

    }
}