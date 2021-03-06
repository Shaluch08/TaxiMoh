package com.uveous.loopfoonpay

import android.Manifest
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import com.uveous.loopfoonpay.Api.ApiClient
import com.uveous.loopfoonpay.Api.ApiService
import com.uveous.loopfoonpay.Api.Model.countrylist
import com.uveous.loopfoonpay.Api.Model.result
import com.uveous.loopfoonpay.Api.Model.userlogin
import com.uveous.loopfoonpay.Api.SessionManager
import com.uveous.taximohdriver.TravelDashboard
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.ArrayList

class LoginActivity : AppCompatActivity()  , LocationListener {

    lateinit var email:EditText
    lateinit var pwd: EditText
    private lateinit var sessionManager: SessionManager
    var listcountryall = ArrayList<result>()
    var listcountry = ArrayList<String>()
    var listcountryid = ArrayList<Int>()
    lateinit var code:LinearLayout
    lateinit var flag:ImageView
    lateinit var countrycode:TextView
    lateinit var dialog : Dialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        email=findViewById(R.id.email)
        pwd=findViewById(R.id.pwd)
        sessionManager = SessionManager(this)
        code=findViewById(R.id.code)
        flag=findViewById(R.id.flag)
        countrycode=findViewById(R.id.countrycode)
        dialog = Dialog(this@LoginActivity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        getCountryfun()

        val butlogin= findViewById<Button>(R.id.login)
        val reg= findViewById<TextView>(R.id.reg)
        val forgotpassword= findViewById<TextView>(R.id.forgotpassword)

        butlogin.setOnClickListener(View.OnClickListener {
            loginfun()

        })
        code.setOnClickListener(View.OnClickListener {
            showDialog()
        })
        reg.setOnClickListener(View.OnClickListener {
            val i= Intent(this,SignUp::class.java)
            startActivity(i)
        })
        forgotpassword.setOnClickListener(View.OnClickListener {
            val i= Intent(this,ForgotActivity::class.java)
            startActivity(i)
        })
    }

    fun loginfun(){

        if(email.text.toString().isEmpty()){
            Toast.makeText(this@LoginActivity,"Please enter mobileno", Toast.LENGTH_SHORT).show()
        }else if(pwd.text.toString().isEmpty()){
            Toast.makeText(this@LoginActivity,"Please enter password", Toast.LENGTH_SHORT).show()
        }else {
            try{
            val progressDialog = ProgressDialog(this@LoginActivity)
            // progressDialog.setTitle("Kotlin Progress Bar")
            progressDialog.setMessage("Please wait")
            progressDialog.show()
            progressDialog.setCanceledOnTouchOutside(false)
            var mAPIService: ApiService? = null
            mAPIService = ApiClient.apiService
            mAPIService!!.login(email.text.toString(), pwd.text.toString()).enqueue(object :
                Callback<userlogin> {
                override fun onResponse(call: Call<userlogin>, response: Response<userlogin>) {
                    Log.i("", "post submitted to API." + response.body()!!)
                    if (response.isSuccessful) {
                        val lo:userlogin = response.body()!!
                        if(lo.status==200){
                            lo.api_token?.let { sessionManager.saveAuthToken(it) }
                            lo.user_id?.let { sessionManager.saveuserid(it) }
                            lo.user_name?.let { sessionManager.savename(it) }
                            lo.user_mobile?.let { sessionManager.savenumber(it) }
                            Log.v("dd", sessionManager.fetchAuthToken().toString())

                            progressDialog.dismiss()
                            val mPrefs: SharedPreferences = this@LoginActivity.getSharedPreferences(
                                    "TASK_ID",
                                    Context.MODE_PRIVATE
                            )
                            val prefsEditor = mPrefs.edit()
                            lo.user_id?.let { prefsEditor.putInt("userid", it) }
                            prefsEditor.commit()
                            Toast.makeText(this@LoginActivity, "Login", Toast.LENGTH_SHORT).show()
                            Log.v("dd", "post registration to API" + response.body()!!.toString())
                            val Intent = Intent(applicationContext, TravelDashboard::class.java)
                            startActivity(Intent)
                        }else{
                            progressDialog.dismiss()
                            Toast.makeText(this@LoginActivity,lo.msg, Toast.LENGTH_SHORT).show()
                        }

                    }
                }

                override fun onFailure(call: Call<userlogin>, t: Throwable) {
                    Toast.makeText(this@LoginActivity, t.message, Toast.LENGTH_SHORT).show()
                }
            })
            }catch (e:java.lang.Exception){

            }

        }
    }

    fun statusCheck() {
        val manager =
                getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps()
        }else{

        }
    }

    private fun buildAlertMessageNoGps() {
        val builder: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(this)
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes",
                        DialogInterface.OnClickListener { dialog, id ->
                            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        })
                .setNegativeButton("No",
                        DialogInterface.OnClickListener { dialog, id -> dialog.cancel() })
        val alert: android.app.AlertDialog? = builder.create()
        alert!!.show()
    }
    private fun fn_permission() {
        if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) !== PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    )
            ) {
            } else {
                ActivityCompat.requestPermissions(
                        this, arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION
                ),
                        REQUEST_PERMISSIONS
                )
            }
        } else {
            boolean_permission = true
            fn_getlocation()
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSIONS -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    boolean_permission = true
                    fn_getlocation()
                    statusCheck()
                } else {
                    statusCheck()
                    //Toast.makeText(HomeActivity.this, "Please allow the permission", Toast.LENGTH_LONG).show();
                }
            }
        }
    }


    var isGPSEnable = false
    var isNetworkEnable = false
    var latitude : Double= 0.0
    var longitude:Double = 0.0
    var locationManager: LocationManager? = null
    var location: Location? = null
    private val REQUEST_PERMISSIONS = 100

    private val MY_REQUEST = 1001
    var boolean_permission = false

    private fun fn_getlocation() {
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        isGPSEnable = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
        isNetworkEnable = locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (!isGPSEnable && !isNetworkEnable) {
        } else {
            if (isNetworkEnable) {
                location = null
                //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,);

                if (locationManager != null) {
                    if (ActivityCompat.checkSelfPermission(
                                    this,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                                    this,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return
                    }
                    location =
                            locationManager!!.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    if (location != null) {
                        Log.e("latitude", location!!.latitude.toString() + "")
                        Log.e("longitude", location!!.longitude.toString() + "")
                        latitude = location!!.latitude
                        longitude = location!!.longitude
                        /*     setMarker()
                             startLatlng = LatLng(latitude, longitude)
                             if (marker == null) {
                                 place1 = MarkerOptions().position(startLatlng!!).title("Origin") //new LatLng(27.658143, 85.3199503)
                                 //new LatLng(27.667491, 85.3208583)
                                 marker = mMap!!.addMarker(place1)
                                 markers.add(marker!!)
                                 marker!!.showInfoWindow()
                                 *//*  int padding = 50;
                            LatLngBounds.Builder builder = new LatLngBounds.Builder();
                            for (Marker marker : markers) {
                                builder.include(marker.getPosition());
                            }
                            LatLngBounds bounds = builder.build();
                            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);*//*
                            // mMap.moveCamera(cu);
                            mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(startLatlng, 16f))
                        } else {
                            marker!!.setPosition(startLatlng!!)
                        }
*/
                    }
                }
            } else if (isGPSEnable) {
                location = null
                if (ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACCESS_FINE_LOCATION
                        ) !== PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        ) !== PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0f, this)
                if (locationManager != null) {
                    location = locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (location != null) {
                        Log.e("latitude", location!!.latitude.toString() + "")
                        Log.e("longitude", location!!.longitude.toString() + "")
                        latitude = location!!.latitude
                        longitude = location!!.longitude
                        // String distance=  UtilityFunction.calculateDistance(latitude,longitude,Double.parseDouble(locationPOJO.getLat()),Double.parseDouble(locationPOJO.getLong()),LocationManager.GPS_PROVIDER);

                        //   fn_update(location);
                        // getNearPlaces();
                        /*  startLatlng = LatLng(latitude, longitude)
                          setMarker()
                          if (marker == null) {
                              place1 = MarkerOptions().position(startLatlng!!).title("Origin") //new LatLng(27.658143, 85.3199503)
                             //new LatLng(27.667491, 85.3208583)
                              marker = mMap!!.addMarker(place1)
                              markers.add(marker!!)
                              marker!!.showInfoWindow()
                              *//*  int padding = 50;
                            LatLngBounds.Builder builder = new LatLngBounds.Builder();
                            for (Marker marker : markers) {
                                builder.include(marker.getPosition());
                            }
                            LatLngBounds bounds = builder.build();
                            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);*//*
                            // mMap.moveCamera(cu);
                            mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(startLatlng, 16f))
                        } else {
                            marker!!.setPosition(startLatlng!!)
                        }
*/
                    }
                }
            }
        }
    }

    override fun onLocationChanged(p0: Location) {
        if (location != null) {
            Log.e("latitude", location!!.latitude.toString() + "")
            Log.e("longitude", location!!.longitude.toString() + "")
            latitude = location!!.latitude
            longitude = location!!.longitude

        }
    }

    private fun getCountryfun() {
        try{
            var mAPIService: ApiService? = null
            mAPIService = ApiClient.apiService
            mAPIService!!.country().enqueue(object : Callback<countrylist> {
                override fun onResponse(call: Call<countrylist>, response: Response<countrylist>) {
                    Log.i("", "post submitted to API." + response.body()!!)
                    if (response.isSuccessful()) {
                        var lo: countrylist = response.body()!!
                        if(lo.status==200){
                            var lo : countrylist =response.body()!!
                            listcountryall=lo.result
                            countrycode.text=lo.result.get(0).phone_code
                            Glide.with(this@LoginActivity).load(lo.result.get(0).icon+"32.png").into(flag)
                            for (i in 0 until listcountryall.size) {
                                listcountryall.get(i).country_name?.let { listcountry.add(it) }
                            }
                            for (i in 0 until listcountryall.size) {
                                listcountryall.get(i).id?.let { listcountryid.add(it) }
                            }

                        }else{

                        }

                    }
                }

                override fun onFailure(call: Call<countrylist>, t: Throwable) {
                    Toast.makeText(this@LoginActivity, t.message, Toast.LENGTH_SHORT).show()
                }
            })
        }catch (e:java.lang.Exception){

        }
    }
    private lateinit var countrylistadapter: CountryListAdapter

    private fun showDialog() {
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.countrycode_layout)
        val window: Window = dialog.getWindow()!!
        window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        val recyclerview = dialog.findViewById(R.id.recyclerview) as RecyclerView
        val layoutManager = LinearLayoutManager(applicationContext)
        recyclerview.layoutManager = layoutManager
        recyclerview.itemAnimator = DefaultItemAnimator()

        countrylistadapter = CountryListAdapter(listcountryall,this@LoginActivity)
        recyclerview.adapter = countrylistadapter

        dialog.show()

    }



    inner class CountryListAdapter(private var counttrycode: List<result>, val context :Context) :
            RecyclerView.Adapter<CountryListAdapter.MyViewHolder>() {

        inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            var countryname: TextView = view.findViewById(R.id.countryname)
            var countrycode: TextView = view.findViewById(R.id.countrycode)
            var flag: ImageView = view.findViewById(R.id.flag)
            var code: LinearLayout = view.findViewById(R.id.code)

        }
        @NonNull
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.recyclr_countrycode, parent, false)
            return MyViewHolder(itemView)
        }
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val counttrycode = counttrycode[position]
            holder.countrycode.text=counttrycode.phone_code
            holder.countryname.text=counttrycode.country_name+" "+"("+counttrycode.country_code+")"
            Glide.with(context).load(counttrycode.icon+"32.png").into(holder.flag)

            holder.code.setOnClickListener(View.OnClickListener {
                dialog.dismiss()
                countrycode.text=counttrycode.phone_code
                Glide.with(this@LoginActivity).load(counttrycode.icon+"32.png").into(flag)
            })
        }

        override fun getItemCount(): Int {
            return counttrycode.size
        }
    }
    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }

}
