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
 * Original version 1.0 of the program.
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
            Bitmap img=null;
            try {
                URL dataSource = new URL(imgUrl[0]);
                HttpURLConnection con = (HttpURLConnection) dataSource.openConnection();
                con.connect();

                img = BitmapFactory.decodeStream(con.getInputStream());
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }

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
                e.printStackTrace();
            }

            dbDownloaded=true;
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
            Pattern p = Pattern.compile("\"name\":\\s*\"(.+?)\"[\\s\\S]*?\"squareImage\":\\s*\"(.*?)\"");
            Matcher m = p.matcher(json);

            while(m.find()) {
                CelebProfile current = null;
                try {
                    current = new CelebProfile(m.group(1), new URL("https:"+m.group(2)));
                } catch (MalformedURLException e) {
                    Log.i("Download","DB Download Failed!!");
                    e.printStackTrace();
                }
                list.add(current);
            }
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
            score++;
            maxStreak=Math.max(score, maxStreak);
        }

        /**
         * Resets score to zero in case of wrong answer.
         */
        void resetScore() {
            score=0;
        }

        /**
         * Constructor that sets up the ArrayList of Celebrity profiles.
         */
        GameEngine() {
            NetConnect fetcher = new NetConnect();
            current = new ArrayList<CelebProfile>();

            try {
                dataStore = fetcher.execute("https://www.forbes.com/ajax/list/data?year=2017&uri=celebrities&type=person")
                                   .get();

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        /**
         * When 97/100 celebrities have already been identified, and thus the game must be reset as enough options aren't available.
         * @return
         */
        CelebProfile refreshGame() {
            for(int i=0;i<=dataStore.size();i++)
                dataStore.get(i).visited=false;
            round=1;
            return getRandom();
        }

        /**
         * Returns a random CelebProfile.
         * @return - a CelebProfile object that will be added to the current set.
         */
        CelebProfile getRandom() {
            int index;
            if(!(round<97)) {
                return refreshGame();
            }

            /**
             * Waiting for the DB to be downloaded and stored in dataStore
             */
            while(!dbDownloaded) try {
                sleep(1000);
                Toast.makeText(MainActivity.this, "Downloading DB...", Toast.LENGTH_SHORT).show();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            while (dataStore.get(index = new Random().nextInt(dataStore.size())).visited)
                continue;
            round++;
            return dataStore.get(index);
        }

        /**
         * Called every time the user guesses a celeb.
         * The answer is chosen here!
         */
        void newSet() {
            for(int i=0;i<4;i++) current.add(getRandom());
            ansIdx = new Random().nextInt(4);   // Randomly selects the pic to display (and the consequent answer).
            current.get(ansIdx).visited = true;
        }
    }

    /**
     * Simply checks if the image was downloaded or not. If not, reloads!
     */
    void testConnection() {
        if(!downloadStatus)
            load();
    }

    /**
     * Called every time the app has to present a new puzzle.
     * Connects the buttons to the answers, and causes an image download which is then assigned as the new Puzzle's celebrity image.
     */
    void load() {
        game.newSet();
        op1.setText(game.current.get(0).name);
        op2.setText(game.current.get(1).name);
        op3.setText(game.current.get(2).name);
        op4.setText(game.current.get(3).name);

        ImgDownloader dlTask = new ImgDownloader();
        try {

            /**
             * This next line is stupid - conversion of text -> URL -> text -> URL wastes resources. Fix it.
             */
            Bitmap currentImg = dlTask.execute(game.current.get(game.ansIdx).profLink.toString()).get();
            celebPic.setImageBitmap(currentImg);
            downloadStatus = true;

        } catch (Exception e) {
            Log.e("Async Task","Can't download Image!");
            e.printStackTrace();
            downloadStatus = false;
        }
    }

    /**
     * Called every time the user choses an option, displays if it was right/wrong and then loads a new puzzle.
     * @param view - contains information about which of the four available buttons was pressed.
     */
    void selected(View view){
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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        game = new GameEngine();
        celebPic = (ImageView) findViewById(R.id.celebPic);
        op1 = (Button) findViewById(R.id.option1);
        op2 = (Button) findViewById(R.id.option2);
        op3 = (Button) findViewById(R.id.option3);
        op4 = (Button) findViewById(R.id.option4);

        load();

        /**
         * Debug below. DELETE!
         */
        Log.d("SubstringB1",op1.getTag().toString().substring(2,3));
        Log.d("SubstringB2",op2.getTag().toString().substring(2,3));
        Log.d("SubstringB3",op3.getTag().toString().substring(2,3));
        Log.d("SubstringB4",op4.getTag().toString().substring(2,3));
    }
}
