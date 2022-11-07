package com.bledemo.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.bledemo.R
import com.bledemo.databinding.ActivityMainBinding
import com.bledemo.ui.view.BaseActivity

class MainActivity : BaseActivity() {

    private lateinit var navController: NavController
    private lateinit var mainBinding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)
        navController = findNavController(R.id.navHostFragment)
        navController.navigate(R.id.fragmentHome)
    }
}