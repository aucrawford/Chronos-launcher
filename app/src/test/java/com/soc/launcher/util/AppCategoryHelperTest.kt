package com.soc.launcher.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AppCategoryHelperTest {

    @Test
    fun testDetermineCategory() {
        assertEquals("Tools", AppCategoryHelper.determineCategory("com.android.chrome"))
        assertEquals("Tools", AppCategoryHelper.determineCategory("org.mozilla.firefox"))
        
        assertEquals("Media", AppCategoryHelper.determineCategory("com.google.android.youtube"))
        assertEquals("Media", AppCategoryHelper.determineCategory("com.netflix.mediaclient"))
        
        assertEquals("Social", AppCategoryHelper.determineCategory("com.whatsapp"))
        assertEquals("Social", AppCategoryHelper.determineCategory("com.facebook.katana"))
        assertEquals("Social", AppCategoryHelper.determineCategory("com.google.android.gm"))
        
        assertEquals("Work", AppCategoryHelper.determineCategory("com.google.android.apps.docs"))
        assertEquals("Work", AppCategoryHelper.determineCategory("com.microsoft.office.word"))
        
        assertEquals("Games", AppCategoryHelper.determineCategory("com.valvesoftware.android.steam.community"))
        
        assertEquals("News", AppCategoryHelper.determineCategory("com.google.android.apps.magazines"))
        
        assertEquals("Tools", AppCategoryHelper.determineCategory("com.android.settings"))
        assertEquals("Tools", AppCategoryHelper.determineCategory("com.android.calculator2"))
        
        assertEquals("System", AppCategoryHelper.determineCategory("com.example.unknown.app"))
    }
}
