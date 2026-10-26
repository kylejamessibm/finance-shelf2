package com.officefinance.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.UUID

data class Office(val id:String, val name:String, val currency:String, val opening:Long)
data class Record(val id:String, val officeId:String, val kind:String, val title:String, val person:String, val amount:Long, val date:String, val renewal:String)

class FinanceDb(ctx:Context):SQLiteOpenHelper(ctx,"finance_shelf_v5.db",null,1) {
    override fun onCreate(db:SQLiteDatabase) {
        db.execSQL("CREATE TABLE offices(id TEXT PRIMARY KEY,name TEXT NOT NULL,currency TEXT NOT NULL,opening INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE records(id TEXT PRIMARY KEY,office_id TEXT NOT NULL,kind TEXT NOT NULL,title TEXT NOT NULL,person TEXT NOT NULL,amount INTEGER NOT NULL,date TEXT NOT NULL,renewal TEXT NOT NULL)")
        db.execSQL("CREATE INDEX idx_records_office_date ON records(office_id,date)")
    }
    override fun onUpgrade(db:SQLiteDatabase,old:Int,new:Int) {}
    fun offices():List<Office> { val out= mutableListOf<Office>(); readableDatabase.rawQuery("SELECT id,name,currency,opening FROM offices ORDER BY rowid",null).use{ c->while(c.moveToNext())out.add(Office(c.getString(0),c.getString(1),c.getString(2),c.getLong(3))) };return out }
    fun addOffice(name:String,currency:String,opening:Long):String { val id=UUID.randomUUID().toString(); val v=ContentValues().apply{put("id",id);put("name",name);put("currency",currency);put("opening",opening)};writableDatabase.insertOrThrow("offices",null,v);return id }
    fun records(id:String):List<Record> {val out=mutableListOf<Record>();readableDatabase.rawQuery("SELECT id,office_id,kind,title,person,amount,date,renewal FROM records WHERE office_id=? ORDER BY date DESC,rowid DESC",arrayOf(id)).use{c->while(c.moveToNext())out.add(Record(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getLong(5),c.getString(6),c.getString(7)))};return out}
    fun addRecord(officeId:String,kind:String,title:String,person:String,amount:Long,date:String,renewal:String){val v=ContentValues().apply{put("id",UUID.randomUUID().toString());put("office_id",officeId);put("kind",kind);put("title",title);put("person",person);put("amount",amount);put("date",date);put("renewal",renewal)};writableDatabase.insertOrThrow("records",null,v)}
    fun upsertOffice(id:String,name:String,currency:String,opening:Long){val v=ContentValues().apply{put("id",id);put("name",name);put("currency",currency);put("opening",opening)};writableDatabase.insertWithOnConflict("offices",null,v,SQLiteDatabase.CONFLICT_IGNORE)}
    fun upsertRecord(id:String,officeId:String,kind:String,title:String,person:String,amount:Long,date:String,renewal:String){val v=ContentValues().apply{put("id",id);put("office_id",officeId);put("kind",kind);put("title",title);put("person",person);put("amount",amount);put("date",date);put("renewal",renewal)};writableDatabase.insertWithOnConflict("records",null,v,SQLiteDatabase.CONFLICT_IGNORE)}
    fun removeRecord(id:String)=writableDatabase.delete("records","id=?",arrayOf(id))
}
