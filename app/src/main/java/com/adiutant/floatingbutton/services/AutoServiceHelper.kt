package com.adiutant.floatingbutton.services

import android.os.Build


class AutoServiceHelper{
    fun click(x:Int,y:Int)
    {


        autoClickService?.click(x,y)
    }
    fun stopckick()
    {

       /// autoClickService?.stopSelf()
        autoClickService = null

    }
}