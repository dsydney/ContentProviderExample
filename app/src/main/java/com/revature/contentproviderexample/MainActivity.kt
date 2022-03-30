package com.revature.contentproviderexample

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.*
import com.revature.contentproviderexample.ui.theme.ContentProviderExampleTheme
import kotlinx.coroutines.launch
import java.lang.StringBuilder
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //val peopleViewmodel = ViewModelProvider(this).get(PeopleViewmodel::class.java)

        setContent {
            ContentProviderExampleTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    UI()
                    //Main(peopleViewmodel)
                }
            }
        }
    }
}

@SuppressLint("Range")
@Composable
fun UI()
{

    var context= LocalContext.current

    Column(
        Modifier.fillMaxHeight(0.5f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        var name by remember { mutableStateOf("") }
        var result  by remember{ mutableStateOf("")}

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") }
        )
        Button(onClick = {

            val values = ContentValues()

            values.put(RevatureCP.name, name)

            context.contentResolver.insert(RevatureCP.CONTENT_URI, values)

            Toast.makeText(context, "New Record is Inserted", Toast.LENGTH_SHORT).show()

        }) {


            Text(text = "Add New Record")
        }

        Button(onClick = {

            val cursor = context.contentResolver.query(RevatureCP.CONTENT_URI, null, null, null, null)

            if(cursor!!.moveToFirst()) {

                val strBuild = StringBuilder()
                while(!cursor.isAfterLast) {
                    result += "${cursor.getString(cursor.getColumnIndex("id"))}-${cursor.getString(cursor.getColumnIndex("name"))}\n"
                    cursor.moveToNext()
                }

                Log.d("Data", "$result")

            } else {

                Log.d("Data", "No Records found")

            }

        }) {


            Text(text = "Show Records")
        }


    }
}


@Composable
fun ContentProviderUI() {

    var context = LocalContext.current

    Column() {
        var newEntry by rememberSaveable{mutableStateOf("")}

        TextField(
            value = newEntry,
            onValueChange = {newEntry = it },
            label = {
                Text(text = "Enter new")
            }
        )

        Button(onClick = {



        }) {
            Text(text = "Add new")
        }
        Button(onClick = { /*TODO*/ }) {
            Text(text = "Show All")
        }
    }


}

@Composable
fun Main(peopleViewmodel: PeopleViewmodel) {

    val peopleList = peopleViewmodel.fetchAllPeople().observeAsState(arrayListOf())

    Scaffold(

        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {

            Column {

                ExtendedFloatingActionButton(

                    text = {
                           Text(text = "People")
                    },
                    onClick = {
                        val name = UUID.randomUUID().toString()
                        peopleViewmodel.insertPerson(
                            People(
                                name = name,
                                email = "default@mail.com"
                            )
                        )
                    }, icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "image",
                            tint = Color.White
                        )
                    }

                )

            }

        },

        content = {

            LazyColumn(content = {

                items(
                    items = peopleList.value,
                    itemContent = {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {

                                Text(text = "{$it.id}")
                                //Text(text = "{$it.name}")
                                //Text(text = "{$it.email}")

                        }
                    }
                )

            })

        }

    )

}

@Entity(tableName = "people")
data class People(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "name")
    val name: String?,

    @ColumnInfo(name = "email")
    val email: String?

)

@Dao
interface PeopleDao {

    @Query("SELECT * FROM people")
    fun fetchAllPeople(): LiveData<List<People>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(people: People)

    @Query("DELETE FROM People where id = :id")
    suspend fun deletePersonById(id: Int)

    @Query("DELETE FROM people")
    suspend fun deleteAllPeople()

}

@Database(entities = [People::class], version = 1, exportSchema = false)
abstract class AppDatabase: RoomDatabase() {

    abstract fun peopleDao(): PeopleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {

            val tempInstance = INSTANCE
            if (tempInstance != null ) {
                return tempInstance
            }

            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jetpack"
                )
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}

class PeopleRepository(application: Application) {

    private var peopleDao: PeopleDao

    init {

        val database = AppDatabase.getDatabase(application)
        peopleDao = database.peopleDao()

    }

    val readAllPeople: LiveData<List<People>> = peopleDao.fetchAllPeople()
    suspend fun insertPerson(people: People) {

        peopleDao.insertPerson(people)

    }

    suspend fun deletePersonById(id: Int) {

        peopleDao.deletePersonById(id)

    }
/*
    init {

        val database = AppDatabase.getDatabase(application)
        peopleDao = database.peopleDao()

    }

 */

}

class PeopleViewmodel(appObj: Application): AndroidViewModel(appObj) {

    private val peopleRepository: PeopleRepository = PeopleRepository(appObj)
    fun fetchAllPeople(): LiveData<List<People>> {

        return peopleRepository.readAllPeople

    }

    fun insertPerson(people: People) {

        viewModelScope.launch {

            peopleRepository.insertPerson(people = people)

        }

    }

    fun deletePeopleById(id: Int) {

        viewModelScope.launch {

            peopleRepository.deletePersonById(id)

        }

    }

}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ContentProviderExampleTheme {

    }
}