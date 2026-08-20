plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.salvia.aiz"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.salvia.aiz"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    
    // تنظیمات جاوا ۱۷ برای هماهنگی کامل با سرورهای گیت‌هاب
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions { 
        jvmTarget = "17" 
    }
    
    buildFeatures { 
        compose = true 
    }
    
    composeOptions { 
        kotlinCompilerExtensionVersion = "1.5.4" 
    }
    
    packaging { 
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" 
    }
}

dependencies {
    // هسته اندروید و کامپوز
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    
    // بارگذاری عکس (لوگو)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // ارتباط با سرور هوش مصنوعی (Z.ai API)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
}
