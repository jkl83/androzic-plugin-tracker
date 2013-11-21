/*
 * Androzic - android navigation client that uses OziExplorer maps (ozf2, ozfx3).
 * Copyright (C) 2010-2013 Andrey Novikov <http://andreynovikov.info/>
 * 
 * This file is part of Androzic application.
 * 
 * Androzic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Androzic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Androzic. If not, see <http://www.gnu.org/licenses/>.
 */

package com.androzic.plugin.tracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.androzic.data.Tracker;

/**
 * This class helps open, create, and upgrade the database file.
 */
class TrackerDataAccess extends SQLiteOpenHelper
{
	private static final String DATABASE_NAME = "tracker.db";
	private static final int DATABASE_VERSION = 2;
	static final String TABLE_NAME = "trackers";
	private static final String TAG = "TrackerDataAccess";
	/**
	 * ID
	 * <P>
	 * Type: LONG
	 * </P>
	 */
	public static final String _ID = "_id";
	/**
	 * Map object ID (mapping ID to Androzic map objects)
	 * <P>
	 * Type: LONG
	 * </P>
	 */
	public static final String MOID = "moid";
	/**
	 * Title
	 * <P>
	 * Type: TEXT
	 * </P>
	 */
	public static final String TITLE = "title";
	/**
	 * IMEI
	 * <P>
	 * Type: TEXT
	 * </P>
	 */
	public static final String IMEI = "imei";
	/**
	 * Sender
	 * <P>
	 * Type: TEXT
	 * </P>
	 */
	public static final String SENDER = "sender";
	/**
	 * Icon
	 * <P>
	 * Type: TEXT
	 * </P>
	 */
	public static final String ICON = "icon";
	/**
	 * Latitude
	 * <P>
	 * Type: DOUBLE
	 * </P>
	 */
	public static final String LATITUDE = "latitude";
	/**
	 * Longitude
	 * <P>
	 * Type: DOUBLE
	 * </P>
	 */
	public static final String LONGITUDE = "longitude";
	/**
	 * Speed
	 * <P>
	 * Type: FLOAT
	 * </P>
	 */
	public static final String SPEED = "speed";
	/**
	 * Battery level
	 * <P>
	 * Type: INTEGER
	 * </P>
	 */
	public static final String BATTERY = "battery";
	/**
	 * Signal level
	 * <P>
	 * Type: INTEGER
	 * </P>
	 */
	public static final String SIGNAL = "signal";
	/**
	 * The timestamp for when the note was last modified
	 * <P>
	 * Type: LONG (long from System.curentTimeMillis())
	 * </P>
	 */
	public static final String MODIFIED = "modified";

	private static final String[] columnsId = new String[] { _ID };
	private static final String[] columnsAll = new String[] { _ID, MOID, TITLE, ICON, IMEI, SENDER, LATITUDE, LONGITUDE, SPEED, BATTERY, SIGNAL, MODIFIED };

	TrackerDataAccess(Context context)
	{

		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		
		Log.w(TAG, "Constructor DB_VER " + DATABASE_VERSION);
	}

	public long saveTracker(Tracker tracker)
	{
		if (tracker.modified == 0)
			tracker.modified = System.currentTimeMillis();

		SQLiteDatabase db = getWritableDatabase();

		Tracker dbTracker = getTracker(tracker.sender);
		if (dbTracker != null)
		{
			// Preserve user defined properties
			if ("".equals(tracker.name))
				tracker.name = dbTracker.name;
			if ("".equals(tracker.image))
				tracker.image = dbTracker.image;

			// Preserve map object ID if it is no set
			if (tracker.moid == Long.MIN_VALUE)
				tracker.moid = dbTracker.moid;

			// Copy tracker ID
			tracker._id = dbTracker._id;
		}
		
		// Set default name for new tracker
		if ("".equals(tracker.name))
			tracker.name = tracker.sender;
			
		ContentValues values = new ContentValues();
		values.put(MOID, tracker.moid);
		values.put(TITLE, tracker.name);
		values.put(ICON, tracker.image);
		values.put(IMEI, tracker.imei);
		values.put(SENDER, tracker.sender);
		values.put(LATITUDE, tracker.latitude);
		values.put(LONGITUDE, tracker.longitude);
		values.put(SPEED, tracker.speed);
		values.put(BATTERY, tracker.battery);
		values.put(SIGNAL, tracker.signal);
		values.put(MODIFIED, Long.valueOf(tracker.modified));

		if (tracker._id > 0)
		{
			db.update(TrackerDataAccess.TABLE_NAME, values, _ID + " = ?", new String[] { String.valueOf(tracker._id) });
		}
		else
		{
			tracker._id = db.insert(TrackerDataAccess.TABLE_NAME, null, values);
		}
		return tracker._id;
	}

	public void removeTracker(Tracker tracker)
	{
		SQLiteDatabase db = getWritableDatabase();
		Cursor cursor = db.query(TrackerDataAccess.TABLE_NAME, columnsId, SENDER + " = ?", new String[] { tracker.sender }, null, null, null);
		if (cursor.getCount() > 0)
		{
			cursor.moveToFirst();
			long id = cursor.getLong(cursor.getColumnIndex(_ID));
			cursor.close();
			db.delete(TrackerDataAccess.TABLE_NAME, _ID + " = ?", new String[] { String.valueOf(id) });
		}
	}

	public Tracker getTracker(String sender)
	{
		Log.w(TAG, "getTracker(String sender)");
		
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = db.query(TrackerDataAccess.TABLE_NAME, columnsAll, SENDER + " = ?", new String[] { sender }, null, null, null);
		if (cursor.getCount() > 0)
		{
			cursor.moveToFirst();
			Tracker tracker = getTracker(cursor);
			cursor.close();
			return tracker;
		}
		return null;
	}

	public Tracker getTracker(Cursor cursor)
	{
		Log.w(TAG, "getTracker(Cursor cursor)");
		
		Tracker tracker = new Tracker();
		tracker._id = cursor.getLong(cursor.getColumnIndex(_ID));
		tracker.moid = cursor.getLong(cursor.getColumnIndex(MOID));
		tracker.latitude = cursor.getDouble(cursor.getColumnIndex(LATITUDE));
		tracker.longitude = cursor.getDouble(cursor.getColumnIndex(LONGITUDE));
		tracker.speed = cursor.getFloat(cursor.getColumnIndex(SPEED));
		tracker.battery = cursor.getInt(cursor.getColumnIndex(BATTERY));
		tracker.signal = cursor.getInt(cursor.getColumnIndex(SIGNAL));
		tracker.name = cursor.getString(cursor.getColumnIndex(TITLE));
		tracker.imei = cursor.getString(cursor.getColumnIndex(IMEI));
		tracker.sender = cursor.getString(cursor.getColumnIndex(SENDER));
		tracker.image = cursor.getString(cursor.getColumnIndex(ICON));
		tracker.modified = cursor.getLong(cursor.getColumnIndex(MODIFIED));
		return tracker;
	}

	public Cursor getTrackers()
	{	
		SQLiteDatabase db = getReadableDatabase();
		
		return db.query(TrackerDataAccess.TABLE_NAME, columnsAll, null, null, null, null, null);
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		Log.w(TAG, "onCreate");
		
		db.execSQL("CREATE TABLE " + TABLE_NAME + " (" + _ID + " INTEGER PRIMARY KEY," 
													   + MOID + " INTEGER," 
													   + IMEI + " TEXT," 
													   + SENDER + " TEXT NOT NULL UNIQUE," 
													   + TITLE + " TEXT," 
													   + ICON + " TEXT," 
													   + LATITUDE + " REAL," 
													   + LONGITUDE + " REAL," 
													   + SPEED + " REAL," 
													   + BATTERY + " INTEGER," 
													   + SIGNAL + " INTEGER," 
													   + MODIFIED + " INTEGER" 
											      + ");");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		Log.e(TAG, " --- onUpgrade database from " + oldVersion
		          + " to " + newVersion + " version --- ");
		
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
		onCreate(db);
	}
}
