package com.example.somu.guesstheceleb;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Thread.sleep;

/**
 * Version 1.1 of the program.
 */
public class MainActivity extends AppCompatActivity {

    GameEngine game;                        //  Stores the game state.
    ImageView celebPic;                     //  The connector for the actual pic displayed in the game.
    Button op1, op2, op3, op4;              //  Each of the four option buttons.
    boolean downloadStatus=false;           //  Set to true when the pic is downloaded successfully.
    boolean dbDownloaded=false;         //  Indicates whether the data download from the internet has finished yet.

    /**
     * The datastructure that holds each individual Celebrity's data.
     */
    public class CelebProfile {
        String name;
        URL profLink;
        boolean visited=false;

        CelebProfile(String name, URL link) {
            this.name = name;
            this.profLink=link;
            Log.i("Performance","Generated DS for "+name);
        }
    }

    public class ImgDownloader extends AsyncTask<String, Void, Bitmap> {

        /**
         * Asynchronously downloads the image needed to display the puzzle.
         * @param imgUrl - link to the image of the selected celebrity
         * @return - The actual image downloaded form the internet.
         */
        @Override
        protected Bitmap doInBackground(String... imgUrl) {
            Log.d("IMG-URL", imgUrl[0]);
            Log.i("Performance","Image Download Started...");
            Bitmap img=null;
            try {
                URL dataSource = new URL(imgUrl[0]);
                HttpURLConnection con = (HttpURLConnection) dataSource.openConnection();
                con.connect();

                img = BitmapFactory.decodeStream(con.getInputStream());
            } catch (java.io.IOException e) {
                Log.e("Performance", "Error reading Image from the internet!");
                e.printStackTrace();
            }
            Log.i("Performance","Image download finished!");
            return img;
        }
    }

    public class NetConnect extends AsyncTask<String, Void, ArrayList<CelebProfile>> {
        /**
         * Asynchronously downloads the entire celebrity information used here from the internet.
         * @param url - Link to the place where the data is available.
         * @return - The ArrayList of CelebData (the singular unit containing all the data) by passing the downloaded data to a parser function.
         */
        @Override
        protected ArrayList<CelebProfile> doInBackground(String... url) {
            Log.i("Performance","Data-Grepping started...");
            StringBuilder data = new StringBuilder();
            try {
                URL dataSource = new URL(url[0]);
                HttpURLConnection con = (HttpURLConnection) dataSource.openConnection();
                con.connect();

                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String line;
                while ((line=br.readLine())!=null)
                    data.append(line);
            } catch (java.io.IOException e) {
                Log.e("Performance", "Error Reading data from the internet!");
                e.printStackTrace();
            }

            dbDownloaded=true;
            Log.i("Performance","Data download finished!");
            return parser(data.toString());
        }

        /**
         * Gets Raw JSON input and feeds it into an ArrayList of CelebProfile data-structure.
         * @param json - The available data (downloaded from a JSON data-source) that has to be fed to the CelebProfile's ArrayList.
         * @return - The final CelebProfile ArrayList containing the info about all celebs used here.
         */
        public ArrayList<CelebProfile> parser(String json) {
            ArrayList<CelebProfile> list = new ArrayList<CelebProfile>();
            /**
             * The following RegEx originally matched HTML tags but the page made an AJAX 
             * call to obtain JSON data - which we're dealing with directly now to reduce
             * page load time and complexity of RegEx required!
             */
            Log.i("Performance","Parsing Started!");
            Pattern p = Pattern.compile("\"name\":\\s*\"(.+?)\"[\\s\\S]*?\"squareImage\":\\s*\"(.*?)\"");
            Matcher m = p.matcher(json);

            while(m.find()) {
                CelebProfile current = null;
                try {
                    current = new CelebProfile(m.group(1), new URL("https:"+m.group(2)));
                } catch (MalformedURLException e) {
                    Log.e("Performance","Error in DB URL!");
                    e.printStackTrace();
                }
                list.add(current);
            }
            Log.i("Performance","Data Grepping finished!");
            return list;
        }
    }

    public class GameEngine {
        int score=0;
        int maxStreak=0;
        ArrayList<CelebProfile> dataStore;
        ArrayList<CelebProfile> current;
        int ansIdx;
        int round=1;

        /**
         * Increases score in case of right answer.
         */
        void increaseScore() {
            Log.i("Performance","Score increased!");
            score++;
            maxStreak=Math.max(score, maxStreak);
        }

        /**
         * Resets score to zero in case of wrong answer.
         */
        void resetScore() {
            Log.i("Performance","Score Reset!");
            score=0;
        }

        /**
         * Constructor that sets up the ArrayList of Celebrity profiles.
         */
        GameEngine() {
            Log.i("Performance","Geme Engine generation started...");
            NetConnect fetcher = new NetConnect();
            current = new ArrayList<CelebProfile>();

            try {
                Log.i("Performance","Attempting to transfer data to dataStore...");
                dataStore = fetcher.execute("https://www.forbes.com/ajax/list/data?year=2017&uri=celebrities&type=person")
                                   .get();

            } catch (InterruptedException e) {
                Log.e("Performance", "DB Download error!");
                e.printStackTrace();
            } catch (ExecutionException e) {
                Log.e("Performance", "DB Download error!");
                e.printStackTrace();
            }
            Log.i("Performance","Game engine ready!");
        }

        /**
         * When 97/100 celebrities have already been identified, and thus the game must be reset as enough options aren't available.
         * @return
         */
        CelebProfile refreshGame() {
            Log.i("Performance","Game refresh started...");
            for(int i=0;i<=dataStore.size();i++)
                dataStore.get(i).visited=false;
            round=1;
            Log.i("Performance","Done!");
            return getRandom();
        }

        /**
         * Returns a random CelebProfile.
         * @return - a CelebProfile object that will be added to the current set.
         */
        CelebProfile getRandom() {
            Log.i("Performance","Generating a random profile... ROUND: "+round);
            int index;
            if(!(round<97)) {
                return refreshGame();
            }

            Log.i("Performance","Waiting to find an unused celeb profile...");
            while (dataStore.get(index = new Random().nextInt(dataStore.size())).visited) {
                Log.i("Performance", "Already visited "+dataStore.get(index).name+", Skipping!");
            }
            Log.i("Performance","Done!");
            return dataStore.get(index);
        }

        /**
         * Called every time the user guesses a celeb.
         * The answer is chosen here!
         */
        void newSet() {
            Log.i("Performance","Generating a new Set of profiles...");
            for(int i=0;i<4;i++) current.add(getRandom());
            ansIdx = new Random().nextInt(4);   // Randomly selects the pic to display (and the consequent answer).
            round++;
            current.get(ansIdx).visited = true;
            Log.i("Performance", current.get(ansIdx).name+" set to Visisted!");
            Log.i("Performance","Generation complete!");
        }
    }

    /**
     * Simply checks if the image was downloaded or not. If not, reloads!
     */
    void testConnection() {
        Log.i("Performance","Begin connection test...");
        if(!downloadStatus)
            load();
        Log.i("Performance","Done!");
    }

    /**
     * Called every time the app has to present a new puzzle.
     * Connects the buttons to the answers, and causes an image download which is then assigned as the new Puzzle's celebrity image.
     */
    void load() {
        Log.i("Performance","LOAD - reloading puzzle...");
        game.newSet();
        op1.setText(game.current.get(0).name);
        op2.setText(game.current.get(1).name);
        op3.setText(game.current.get(2).name);
        op4.setText(game.current.get(3).name);
        Log.i("Performance","Button Texts changed!");

        ImgDownloader dlTask = new ImgDownloader();
        try {

            /**
             * This next line is stupid - conversion of text -> URL -> text -> URL wastes resources. Fix it.
             */
            Log.i("Performance","Attempting to trigger image download...");
            Bitmap currentImg = dlTask.execute(game.current.get(game.ansIdx).profLink.toString()).get();
            celebPic.setImageBitmap(currentImg);
            downloadStatus = true;
            Log.i("Performance","Image changed!");

        } catch (Exception e) {
            Log.e("Performance","Can't set Downloaded image!");
            e.printStackTrace();
            downloadStatus = false;
        }
    }

    /**
     * Called every time the user choses an option, displays if it was right/wrong and then loads a new puzzle.
     * @param view - contains information about which of the four available buttons was pressed.
     */
    void selected(View view){
        Log.i("Performance","Button Clicked!");
        int ans = Integer.parseInt(view.getTag().toString().substring(2,3));
        if(--ans==game.ansIdx) {
            game.increaseScore();
            Toast.makeText(this, "You were absolutely right!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Sorry, you were wrong! The right answer was : "+game.current.get(game.ansIdx).name, Toast.LENGTH_LONG).show();
            game.resetScore();
        }
        load();
    }

    /**
     * Just links the ImageView and buttons to their counterpart in code and then loads the first puzzle.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("Performance","App Setup started...");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        game = new GameEngine();
        celebPic = (ImageView) findViewById(R.id.celebPic);
        op1 = (Button) findViewById(R.id.option1);
        op2 = (Button) findViewById(R.id.option2);
        op3 = (Button) findViewById(R.id.option3);
        op4 = (Button) findViewById(R.id.option4);

        Log.i("Performance","Starting Load...");
        load();
    }
}
