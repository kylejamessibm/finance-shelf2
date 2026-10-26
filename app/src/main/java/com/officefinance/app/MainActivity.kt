package com.officefinance.app

import android.app.*
import android.os.Bundle
import android.content.*
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.widget.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.text.TextWatcher
import android.text.Editable
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class MainActivity:Activity() {
    private val navy=Color.rgb(17,40,63)
    private val teal=Color.rgb(32,148,131)
    private val paper=Color.rgb(244,248,249)
    private val muted=Color.rgb(106,123,128)
    private lateinit var db:FinanceDb
    private lateinit var content:LinearLayout
    private var officeId:String?=null
    private var month=YearMonth.now()
    private var csv:String=""
    private var screen="home"
    private var filter="all"
    private var query=""
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val cloud by lazy { FirebaseFirestore.getInstance() }
    private val PREF="finance_shelf_session"
    private val dateFmt=DateTimeFormatter.ofPattern("d MMMM yyyy",Locale.ENGLISH)
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);db=FinanceDb(this);window.statusBarColor=navy;window.navigationBarColor=navy; splash()}
    private fun d(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun bg(color:Int,r:Int=18)=GradientDrawable().apply{setColor(color);cornerRadius=d(r).toFloat()}
    private fun column()=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
    private fun row()=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
    private fun text(value:String,size:Float=15f,color:Int=navy,bold:Boolean=false)=TextView(this).apply{text=value;textSize=size;setTextColor(color);if(bold)setTypeface(null,Typeface.BOLD)}
    private fun gap(parent:LinearLayout,h:Int=12)=parent.addView(Space(this),LinearLayout.LayoutParams(1,d(h)))
    private fun padded(v:View,p:Int=16){v.setPadding(d(p),d(p),d(p),d(p))}
    private fun panel(parent:LinearLayout):LinearLayout {
        val l=column();padded(l);l.background=bg(Color.WHITE,18)
        l.elevation=d(2).toFloat()
        parent.addView(l,LinearLayout.LayoutParams(-1,-2));return l
    }
    private fun root():LinearLayout {val scroll=ScrollView(this).apply{setBackgroundColor(paper);isFillViewport=true};content=column();padded(content,18);scroll.addView(content);setContentView(scroll);return content}
    private fun action(label:String,primary:Boolean=true,fn:()->Unit)=Button(this).apply{
        text=label;isAllCaps=false;textSize=13f;typeface=Typeface.DEFAULT_BOLD
        setTextColor(if(primary)Color.WHITE else navy)
        background=bg(if(primary)navy else Color.rgb(226,242,239),14)
        minHeight=d(48);elevation=0f;setOnClickListener{fn()}
    }
    private fun input(hint:String)=EditText(this).apply{this.hint=hint;textSize=15f;setSingleLine(true);backgroundTintList=android.content.res.ColorStateList.valueOf(teal);minHeight=d(48)}
    private fun field(parent:LinearLayout,name:String,hint:String):EditText{parent.addView(text(name,13f,muted,true));val edit=input(hint);parent.addView(edit);gap(parent,10);return edit}
    private fun heading(parent:LinearLayout,title:String,subtitle:String="") { parent.addView(text(title,25f,navy,true));if(subtitle.isNotEmpty()){gap(parent,4);parent.addView(text(subtitle,13f,muted))};gap(parent,16) }
    private fun divider(parent:LinearLayout){parent.addView(View(this).apply{setBackgroundColor(Color.rgb(234,239,239))},LinearLayout.LayoutParams(-1,d(1)))}
    private fun section(parent:LinearLayout,title:String){gap(parent,8);parent.addView(text(title,17f,navy,true));gap(parent,9)}
    private fun nav(parent:LinearLayout,selected:String){
        gap(parent,24)
        val bar=row().apply{background=bg(Color.WHITE,18);setPadding(d(5),d(7),d(5),d(7));elevation=d(3).toFloat()}
        val items=listOf(Triple("⌂","Home","home"),Triple("▤","Records","records"),Triple("◷","Renewals","renewals"),Triple("⚙","Settings","settings"))
        for((symbol,label,key) in items){
            val tab=column().apply{
                gravity=Gravity.CENTER
                background=bg(if(selected==key)Color.rgb(222,244,238) else Color.WHITE,13)
                setPadding(d(2),d(8),d(2),d(8))
            }
            tab.addView(text(symbol,21f,if(selected==key)teal else muted,true).apply{gravity=Gravity.CENTER})
            tab.addView(text(label,10f,if(selected==key)navy else muted,selected==key).apply{gravity=Gravity.CENTER})
            tab.setOnClickListener{when(key){"home"->dashboard();"records"->transactions();"renewals"->renewals();"settings"->settings()}}
            bar.addView(tab,LinearLayout.LayoutParams(0,d(62),1f))
        }
        parent.addView(bar)
    }
    private fun eyebrow(parent:LinearLayout,label:String){parent.addView(text(label.uppercase(Locale.ENGLISH),11f,teal,true));gap(parent,4)}
    private fun emptyState(parent:LinearLayout,title:String,description:String){
        val p=panel(parent);p.addView(text("◇",26f,teal,true));gap(p,5)
        p.addView(text(title,16f,navy,true));gap(p,3);p.addView(text(description,13f,muted))
    }
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_LONG).show()
    private fun splash(){
        val c=root();c.setBackgroundColor(navy);gap(c,105)
        val mark=ImageView(this).apply{setImageResource(R.drawable.finance_mark);scaleType=ImageView.ScaleType.FIT_CENTER}
        c.addView(mark,LinearLayout.LayoutParams(-1,d(155)));gap(c,15)
        c.addView(text("Finance Shelf",30f,Color.WHITE,true).apply{gravity=Gravity.CENTER})
        gap(c,7);c.addView(text("Every account. One place.",14f,Color.rgb(132,220,200)).apply{gravity=Gravity.CENTER})
        mark.alpha=0f;mark.scaleX=.85f;mark.scaleY=.85f;mark.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(650).start()
        android.os.Handler(mainLooper).postDelayed({if(!isFinishing)welcomeOrOffice()},1100)
    }
    private fun welcomeOrOffice(){val saved=getPreferences(MODE_PRIVATE).getBoolean("started",false);if(!saved)welcome() else offices()}
    private fun welcome(){val c=root();gap(c,30)
        val hero=column().apply{background=bg(navy,24);setPadding(d(18),d(28),d(18),d(28))}
        hero.addView(ImageView(this).apply{setImageResource(R.drawable.finance_mark);scaleType=ImageView.ScaleType.FIT_CENTER},LinearLayout.LayoutParams(-1,d(125)))
        hero.addView(text("Finance Shelf",28f,Color.WHITE,true).apply{gravity=Gravity.CENTER});gap(hero,5)
        hero.addView(text("Every account. One place.",13f,Color.rgb(143,219,204)).apply{gravity=Gravity.CENTER});c.addView(hero);gap(c,24)
        heading(c,"Welcome","Manage offices, money, digital wallets and renewals in one place.")
        c.addView(action("Continue with Google"){googleLogin()});gap(c,10)
        c.addView(action("Continue as Guest",false){getPreferences(MODE_PRIVATE).edit().putBoolean("started",true).apply();offices()})
        gap(c,14);c.addView(text("Guest mode works locally. Connect Google anytime in Settings to back up your records.",12f,muted))
    }
    private fun offices(){officeId=null;val c=root();heading(c,"My Offices","Every company has its own financial workspace.")
        val list=db.offices();if(list.isEmpty()){val empty=panel(c);empty.addView(text("Start with your first office",18f,navy,true));gap(empty,8);empty.addView(text("Name your company, pick a currency and start tracking.",14f,muted));gap(c,14)}
        for(o in list){val card=panel(c);val r=row();val symbol=text("▦",28f,teal,true);r.addView(symbol,LinearLayout.LayoutParams(d(45),-2));val detail=column();detail.addView(text(o.name,17f,navy,true));detail.addView(text("${o.currency}  •  ${db.records(o.id).size} transactions",12f,muted));r.addView(detail,LinearLayout.LayoutParams(0,-2,1f));r.addView(text("›",27f,teal,true));card.addView(r);card.setOnClickListener{officeId=o.id;dashboard()};gap(c,10)}
        c.addView(action("+ Create New Office"){newOffice()});gap(c,12)
        c.addView(action("Backup & Account",false){settings()});gap(c,10)
        c.addView(text(if(auth.currentUser!=null)"Google connected" else "Guest mode • Local storage",12f,muted))
    }
    private fun newOffice(){val c=root();c.addView(text("‹  My Offices",14f,teal,true).apply{setOnClickListener{offices()}});gap(c,18);c.addView(text("Add New Office",25f,navy,true));gap(c,8);c.addView(text("Your company name becomes your project name.",14f,muted));gap(c,20);val box=panel(c);val name=field(box,"Office / Company name *","e.g. Global Media Office");box.addView(text("Default currency",13f,muted,true));val spinner=Spinner(this);val currencies=listOf("PKR","USD","GBP","EUR");spinner.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,currencies);box.addView(spinner);gap(box,12);val opening=field(box,"Opening balance (optional)","0.00");opening.inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL;gap(c,20);c.addView(action("Create Office"){val n=name.text.toString().trim();val amount=opening.text.toString().trim().toDoubleOrNull()?:0.0;if(n.isEmpty()){name.error="Required";return@action};if(amount<0){opening.error="Use zero or greater";return@action};officeId=db.addOffice(n,currencies[spinner.selectedItemPosition],(amount*100).toLong());uploadAfterChange();dashboard()})}
    private fun chosen():Office?=db.offices().firstOrNull{it.id==officeId}
    private fun cash(v:Long,c:String):String="${c} ${"%,.2f".format(Locale.US,v/100.0)}"
    private fun dashboard(){
        val o=chosen()?:return offices()
        val c=root()
        val top=row()
        val back=text("‹",30f,navy,true).apply{setOnClickListener{offices()}}
        top.addView(back,LinearLayout.LayoutParams(d(35),-2))
        val headingBox=column()
        eyebrow(headingBox,"YOUR WORKSPACE")
        headingBox.addView(text(o.name,23f,navy,true))
        top.addView(headingBox,LinearLayout.LayoutParams(0,-2,1f))
        top.addView(text("⚙",23f,teal,true).apply{setPadding(d(8),d(8),d(8),d(8));setOnClickListener{settings()}})
        c.addView(top);gap(c,18)
        val monthNav=row().apply{background=bg(Color.WHITE,15);setPadding(d(4),d(3),d(4),d(3))}
        monthNav.addView(action("‹",false){month=month.minusMonths(1);dashboard()},LinearLayout.LayoutParams(d(48),d(44)))
        monthNav.addView(text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy",Locale.ENGLISH)),15f,navy,true).apply{gravity=Gravity.CENTER},LinearLayout.LayoutParams(0,d(44),1f))
        monthNav.addView(action("›",false){month=month.plusMonths(1);dashboard()},LinearLayout.LayoutParams(d(48),d(44)))
        c.addView(monthNav);gap(c,14)
        val records=db.records(o.id)
        val shown=records.filter{it.date.startsWith(month.toString())}
        val income=shown.filter{it.kind=="income"}.sumOf{it.amount}
        val spent=shown.filter{it.kind=="expense"}.sumOf{it.amount}
        val carry=o.opening+records.filter{it.date.take(7)<month.toString()}.sumOf{if(it.kind=="income")it.amount else -it.amount}
        val balance=column().apply{
            background=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(navy,Color.rgb(22,72,77))).apply{cornerRadius=d(23).toFloat()}
            setPadding(d(22),d(23),d(20),d(24))
        }
        balance.addView(text("TOTAL AVAILABLE",11f,Color.rgb(181,216,216),true));gap(balance,8)
        balance.addView(text(cash(carry+income-spent,o.currency),29f,Color.WHITE,true));gap(balance,18)
        val synced=auth.currentUser!=null
        balance.addView(text(if(synced)"☁ Google connected · Manual backup" else "◉ Guest mode · Stored on this phone",12f,Color.rgb(161,225,207)))
        gap(balance,5)
        balance.addView(text("Carried forward ${cash(carry,o.currency)}",11f,Color.rgb(201,222,225)))
        c.addView(balance);gap(c,12)
        val stats=row()
        fun mini(label:String,value:Long,positive:Boolean){
            val card=column().apply{background=bg(if(positive)Color.rgb(224,245,238) else Color.WHITE,17);setPadding(d(14),d(15),d(10),d(15))}
            card.addView(text(if(positive)"↙  RECEIVED" else "↗  EXPENSES",11f,if(positive)teal else muted,true));gap(card,8)
            card.addView(text(cash(value,o.currency),17f,navy,true))
            stats.addView(card,LinearLayout.LayoutParams(0,d(90),1f))
        }
        mini("Received",income,true)
        stats.addView(Space(this),LinearLayout.LayoutParams(d(10),1));mini("Expenses",spent,false)
        c.addView(stats);gap(c,19)
        eyebrow(c,"QUICK ENTRY")
        val actions=row()
        actions.addView(action("+ Expense"){newEntry("expense")},LinearLayout.LayoutParams(0,d(50),1f))
        actions.addView(Space(this),LinearLayout.LayoutParams(d(10),1))
        actions.addView(action("+ Received",false){newEntry("income")},LinearLayout.LayoutParams(0,d(50),1f))
        c.addView(actions);gap(c,16)
        val shortcuts=row()
        for((label,callback) in listOf<Pair<String,()->Unit>>("Transactions" to {transactions()},"Renewals" to {renewals()},"Export CSV" to {exportCsv(o,shown)})){
            val b=action(label,false,callback);b.textSize=11f
            shortcuts.addView(b,LinearLayout.LayoutParams(0,d(43),1f))
        }
        c.addView(shortcuts)
        section(c,"Recent activity")
        if(shown.isEmpty())emptyState(c,"Nothing recorded yet","Add your first payment or expense to get started.")
        shown.take(5).forEach{transactionRow(c,it,o)}
        if(shown.size>5)c.addView(action("View all transactions",false){transactions()})
        section(c,"Upcoming renewals")
        val upcoming=records.filter{it.renewal.isNotBlank()&&it.kind=="expense"}.sortedBy{it.renewal}.take(3)
        if(upcoming.isEmpty())emptyState(c,"No renewals yet","Renewal dates you add will appear here.")
        upcoming.forEach{r->val card=panel(c);val rr=row();val detail=column();detail.addView(text(r.title,14f,navy,true));gap(detail,4);detail.addView(text(r.person.ifBlank{"General"},12f,muted));rr.addView(detail,LinearLayout.LayoutParams(0,-2,1f));rr.addView(text(displayDate(r.renewal),12f,teal,true));card.addView(rr);gap(c,8)}
        nav(c,"home")
    }
    private fun transactionRow(c:LinearLayout,r:Record,o:Office){
        val rr=row().apply{background=bg(Color.WHITE,13);setPadding(d(11),d(12),d(11),d(12))}
        val mark=text(if(r.kind=="income")"↙" else "↗",21f,teal,true).apply{
            gravity=Gravity.CENTER;background=bg(Color.rgb(228,245,239),12)
        }
        rr.addView(mark,LinearLayout.LayoutParams(d(42),d(42)))
        rr.addView(Space(this),LinearLayout.LayoutParams(d(10),1))
        val left=column()
        left.addView(text(r.title,14f,navy,true))
        left.addView(text("${r.person.ifBlank{"General"}} · ${displayDate(r.date)}",11f,muted))
        rr.addView(left,LinearLayout.LayoutParams(0,-2,1f))
        rr.addView(text((if(r.kind=="income")"+" else "−")+cash(r.amount,o.currency),12f,if(r.kind=="income")teal else navy,true))
        c.addView(rr);rr.setOnClickListener{detail(r)};gap(c,6)
    }
    private fun detail(r:Record){AlertDialog.Builder(this).setTitle(r.title).setMessage("${if(r.kind=="income")"Received" else "Expense"} • ${r.person.ifBlank{"General"}}\n${displayDate(r.date)}\nRenewal: ${if(r.renewal.isBlank())"None" else displayDate(r.renewal)}").setNegativeButton("Close",null).setPositiveButton("Delete"){_,_->AlertDialog.Builder(this).setTitle("Delete transaction?").setPositiveButton("Delete"){_,_->db.removeRecord(r.id);uploadAfterChange();dashboard()}.setNegativeButton("Cancel",null).show()}.show()}
    private fun transactions(){val o=chosen()?:return offices();val c=root();heading(c,"Transactions",o.name);val all=db.records(o.id)
        val search=input("Search service, project or person").apply{setText(query)};c.addView(search);gap(c,10)
        val controls=row();for((label,value) in listOf("All" to "all","Expense" to "expense","Received" to "income")){val b=action(label,filter==value){filter=value;query=search.text.toString();transactions()};b.textSize=12f;controls.addView(b,LinearLayout.LayoutParams(0,d(46),1f))};c.addView(controls);gap(c,12)
        val list=column();c.addView(list);fun draw(){list.removeAllViews();val q=search.text.toString().trim();val filtered=all.filter{(filter=="all"||it.kind==filter)&&(q.isEmpty()||it.title.contains(q,true)||it.person.contains(q,true))};if(filtered.isEmpty())list.addView(text("No matching transactions",14f,muted));filtered.forEach{transactionRow(list,it,o)}}
        search.addTextChangedListener(object:TextWatcher{override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){};override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){draw()};override fun afterTextChanged(s:Editable?){}});draw()
        gap(c,10);c.addView(action("Export All to CSV",false){exportCsv(o,all)});nav(c,"records")
    }
    private fun renewals(){val o=chosen()?:return offices();val c=root();heading(c,"Renewals",o.name);val all=db.records(o.id).filter{it.kind=="expense"&&it.renewal.isNotBlank()}.sortedBy{it.renewal};if(all.isEmpty()){val e=panel(c);e.addView(text("No scheduled renewals",16f,navy,true));e.addView(text("Add a renewal date while recording a subscription expense.",13f,muted))};val today=LocalDate.now().toString();for(r in all){val card=panel(c);card.addView(text(r.title,16f,navy,true));card.addView(text("${r.person.ifBlank{"General"}} • ${cash(r.amount,o.currency)}",12f,muted));gap(card,4);card.addView(text((if(r.renewal<today)"Overdue: " else "Due: ")+displayDate(r.renewal),13f,teal,true));gap(c,10)};nav(c,"renewals")}
    private fun settings(){val c=root();heading(c,"Settings & Backup","Account, storage and recovery")
        val account=panel(c);account.addView(text("Account",18f,navy,true));gap(account,8);account.addView(text(if(auth.currentUser==null)"Guest mode · saved on this phone" else "Connected: ${auth.currentUser?.email ?: "Google Account"}",13f,muted));gap(account,12)
        if(auth.currentUser==null){account.addView(action("Connect Google for Backup"){googleLogin()})}else{account.addView(action("Back Up to Cloud"){backupToCloud()});gap(account,7);account.addView(action("Restore Cloud Data",false){confirmRestore()});gap(account,7);account.addView(action("Sign Out",false){auth.signOut();toast("Signed out. Local records remain on device.");settings()})}
        gap(c,14);val backup=panel(c);backup.addView(text("Google Cloud Backup (Beta)",18f,navy,true));gap(backup,6);backup.addView(text("Connect Google to back up your offices and transactions. A completed backup can be restored on another phone. This beta does not upload payment proof images.",13f,muted));gap(backup,8);backup.addView(text("Last backup: "+getPreferences(MODE_PRIVATE).getString("last_backup","Not backed up yet"),12f,teal,true));gap(c,14)
        val options=panel(c);options.addView(text("Manage Offices",17f,navy,true));gap(options,9);options.addView(action("Switch / Add Office",false){offices()});gap(c,11);options.apply{addView(text("Export transactions",17f,navy,true));gap(this,8);addView(action("Export Current Office (CSV)",false){val o=chosen();if(o!=null)exportCsv(o,db.records(o.id)) else toast("Choose an office first")})};gap(c,12)
        c.addView(text("Note: Google login may require the correct SHA-1 signing fingerprint. Check before relying on cloud recovery.",12f,muted));nav(c,"settings")
    }
    private fun googleLogin(){val webId=getString(R.string.default_web_client_id);val options=GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(webId).requestEmail().build();val client=GoogleSignIn.getClient(this,options);startActivityForResult(client.signInIntent,1031)}
    private fun uploadAfterChange(){ /* Automatic snapshot overwrite disabled until safe per-record sync is implemented. */ }
    private fun backupToCloud(show:Boolean=true){val user=auth.currentUser ?: run { toast("Please connect Google first"); return };val offices=db.offices();val payload=hashMapOf<String,Any>("schema" to 1,"offices" to offices.map{mapOf("id" to it.id,"name" to it.name,"currency" to it.currency,"opening" to it.opening)},"records" to offices.flatMap{o->db.records(o.id).map{r->mapOf("id" to r.id,"officeId" to r.officeId,"kind" to r.kind,"title" to r.title,"person" to r.person,"amount" to r.amount,"date" to r.date,"renewal" to r.renewal)}} ,"updatedAt" to com.google.firebase.Timestamp.now())
        cloud.collection("users").document(user.uid).collection("settings").document("snapshot").set(payload).addOnSuccessListener{getPreferences(MODE_PRIVATE).edit().putString("last_backup",java.time.LocalDateTime.now().toString().take(16).replace('T',' ')).apply();if(show)toast("Cloud backup saved")}.addOnFailureListener{if(show)toast("Backup failed: ${it.localizedMessage}")}
    }
    private fun confirmRestore(){AlertDialog.Builder(this).setTitle("Restore cloud backup?").setMessage("This will ADD missing offices and transactions from your cloud backup. Existing records will not be deleted. Confirm before proceeding.").setNegativeButton("Cancel",null).setPositiveButton("Restore"){_,_->restoreCloud()}.show()}
    private fun restoreCloud(){val user=auth.currentUser?:return;cloud.collection("users").document(user.uid).collection("settings").document("snapshot").get().addOnSuccessListener{snap->if(!snap.exists()){toast("No cloud backup found");return@addOnSuccessListener};try{val os=snap.get("offices") as? List<*> ?: emptyList<Any>();val rs=snap.get("records") as? List<*> ?: emptyList<Any>();for(v in os){val m=v as? Map<*,*> ?:continue;db.upsertOffice(m["id"].toString(),m["name"].toString(),m["currency"].toString(),(m["opening"] as Number).toLong())};for(v in rs){val m=v as? Map<*,*> ?:continue;db.upsertRecord(m["id"].toString(),m["officeId"].toString(),m["kind"].toString(),m["title"].toString(),m["person"].toString(),(m["amount"] as Number).toLong(),m["date"].toString(),m["renewal"].toString())};toast("Restore complete");offices()}catch(e:Exception){toast("Restore failed: ${e.localizedMessage}")}}.addOnFailureListener{toast("Restore failed: ${it.localizedMessage}")}}
    private fun displayDate(s:String):String=try{LocalDate.parse(s).format(dateFmt)}catch(e:Exception){s}
    private fun newEntry(kind:String){val o=chosen()?:return offices();val c=root();c.addView(text(if(kind=="income")"Add Received Payment" else "Add Expense",24f,navy,true));gap(c,14);val p=panel(c);val title=field(p,if(kind=="income")"Payment source *" else "Service name *",if(kind=="income")"Office funding" else "ChatGPT Plus");val owner=field(p,"Person / Project (optional)","e.g. Ahmed / Project A");val amount=field(p,"Amount (${o.currency}) *","0.00");amount.inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL;val date=field(p,"Payment date","YYYY-MM-DD");date.setText(LocalDate.now().toString());date.setOnClickListener{datePicker(date)};date.isFocusable=false;val renewal=field(p,"Renewal date (optional)","Choose a date");renewal.setOnClickListener{datePicker(renewal)};renewal.isFocusable=false;gap(c,15);c.addView(action("Save ${if(kind=="income")"Payment" else "Expense"}"){val n=title.text.toString().trim();val amt=amount.text.toString().toDoubleOrNull();if(n.isEmpty()){title.error="Required";return@action};if(amt==null||amt<=0){amount.error="Enter a valid amount";return@action};val day=try{LocalDate.parse(date.text.toString()).toString()}catch(e:Exception){date.error="Invalid date";return@action};db.addRecord(o.id,kind,n,owner.text.toString().trim(),(amt*100).toLong(),day,renewal.text.toString());uploadAfterChange();month=YearMonth.parse(day.take(7));dashboard()})}
    private fun datePicker(e:EditText){val now=LocalDate.now();DatePickerDialog(this,{_,y,m,day->e.setText(LocalDate.of(y,m+1,day).toString())},now.year,now.monthValue-1,now.dayOfMonth).show()}
    private fun exportCsv(o: Office, items: List<Record>) {
        fun escapeCsv(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""

        val header = "Date,Type,Service,Person/Project,Amount,Currency,Renewal"
        val rows = items.joinToString("\n") { record ->
            listOf(
                record.date,
                record.kind,
                record.title,
                record.person,
                (record.amount / 100.0).toString(),
                o.currency,
                record.renewal
            ).joinToString(",") { value -> escapeCsv(value) }
        }
        csv = header + "\n" + rows

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
            putExtra(Intent.EXTRA_TITLE, "finance-shelf-${month}.csv")
        }
        startActivityForResult(intent, 1020)
    }
    @Deprecated("Legacy activity result for document picker")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==1031&&resultCode==RESULT_OK){try{val acct=GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java);val token=acct.idToken ?: throw Exception("Missing Google token");auth.signInWithCredential(GoogleAuthProvider.getCredential(token,null)).addOnSuccessListener{getPreferences(MODE_PRIVATE).edit().putBoolean("started",true).apply();AlertDialog.Builder(this).setTitle("Google Connected").setMessage("Back up existing guest data now, or restore data saved on another phone. Choose carefully to avoid overwriting a cloud snapshot.").setPositiveButton("Backup this phone"){_,_->backupToCloud();offices()}.setNegativeButton("Restore from cloud"){_,_->confirmRestore();offices()}.show()}.addOnFailureListener{toast("Google authentication failed: ${it.localizedMessage}")}}catch(e:Exception){toast("Sign in failed: ${e.localizedMessage}")};return}
        if(requestCode==1020&&resultCode==RESULT_OK){try{data?.data?.let{contentResolver.openOutputStream(it)?.use{stream->stream.write(csv.toByteArray(Charsets.UTF_8))}};toast("CSV exported")}catch(e:Exception){toast("Export failed: ${e.message}")}}}
}
