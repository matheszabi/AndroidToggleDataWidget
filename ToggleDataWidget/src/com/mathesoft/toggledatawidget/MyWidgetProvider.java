package com.mathesoft.toggledatawidget;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * 
 * @author matheszabi
 * 
 */
public class MyWidgetProvider extends AppWidgetProvider {
	private static final String TAG = "MyWidgetProvider";
	private static final String CLICK_ACTION = "com.mathesoft.toggledatawidget.action.CLICK";

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.appwidget.AppWidgetProvider#onReceive(android.content.Context, android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent) {

		final String action = intent.getAction();

		if (action.equals(CLICK_ACTION)) {
			// set a temp design for be responsive:
			AppWidgetManager man = AppWidgetManager.getInstance(context);
			
			RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
			rv.setImageViewResource(R.id.imageView, ( R.drawable.arrows_action));
			int[] ids = man.getAppWidgetIds(new ComponentName(context, MyWidgetProvider.class));
			for (int i = 0; i < ids.length; ++i){
				man.updateAppWidget(ids, rv);
			}
			// do the data switch:
			boolean isEnabled = isMobilDataEnabled(context);
			//Log.d(TAG, "onReceive() -clicked: isEnabled:" + isEnabled);
			setMobileDataEnabled(context, !isEnabled);
		} else if (action.equals("android.net.conn.CONNECTIVITY_CHANGE")) {
			Intent intent2 = new Intent(context, MyWidgetProvider.class);
			intent2.setAction("android.appwidget.action.APPWIDGET_UPDATE");

			AppWidgetManager man = AppWidgetManager.getInstance(context);
			int[] ids = man.getAppWidgetIds(new ComponentName(context, MyWidgetProvider.class));

			intent2.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
			context.sendBroadcast(intent2);
		}

		super.onReceive(context, intent);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.appwidget.AppWidgetProvider#onUpdate(android.content.Context, android.appwidget.AppWidgetManager, int[])
	 */
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

		for (int i = 0; i < appWidgetIds.length; ++i) {
			RemoteViews layout = buildLayout(context, appWidgetIds[i]);
			appWidgetManager.updateAppWidget(appWidgetIds[i], layout);
		}
		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}

	@SuppressWarnings("rawtypes")
	private boolean isMobilDataEnabled(Context context) {

		boolean isEnabled = false;
		ConnectivityManager conMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		try {
			final Class conmanClass = Class.forName(conMgr.getClass().getName());
			final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
			iConnectivityManagerField.setAccessible(true);
			final Object iConnectivityManager = iConnectivityManagerField.get(conMgr);
			final Class<?> iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
			final Method getMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("getMobileDataEnabled");
			getMobileDataEnabledMethod.setAccessible(true);

			Object result = getMobileDataEnabledMethod.invoke(iConnectivityManager);
			if (result instanceof Boolean) {
				isEnabled = (Boolean) result;
			}

		} catch (Exception ex) {
			Log.e(TAG, "", ex);
		}

		return isEnabled;
	}

	private void setMobileDataEnabled(Context context, boolean enabled) {
		try {
			final ConnectivityManager conman = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			final Class<?> conmanClass = Class.forName(conman.getClass().getName());
			final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
			iConnectivityManagerField.setAccessible(true);
			final Object iConnectivityManager = iConnectivityManagerField.get(conman);
			final Class<?> iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
			final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
			setMobileDataEnabledMethod.setAccessible(true);

			setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);
		} catch (Exception ex) {
			Log.e(TAG, "Can't switch to: " + enabled, ex);
		}
	}

	private RemoteViews buildLayout(Context context, int appWidgetId) {

		RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
		final Intent onClickIntent = new Intent(context, MyWidgetProvider.class);
		onClickIntent.setAction(MyWidgetProvider.CLICK_ACTION);
		onClickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		onClickIntent.setData(Uri.parse(onClickIntent.toUri(Intent.URI_INTENT_SCHEME)));

		final PendingIntent onClickPendingIntent = PendingIntent.getBroadcast(context, 0, onClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		rv.setOnClickPendingIntent(R.id.imageView, onClickPendingIntent);

		boolean isEnabled = isMobilDataEnabled(context);
		rv.setImageViewResource(R.id.imageView, (isEnabled ? R.drawable.arrows : R.drawable.arrows_disabled));

		return rv;
	}

}
