package fusionkey.lowkeyfinal.main;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.json.JSONObject;

import fusionkey.lowkeyfinal.ChatActivity;
import fusionkey.lowkeyfinal.queue.LobbyCheckerRunnable;
import fusionkey.lowkeyfinal.queue.QueueMatcher;

import static com.android.volley.Request.Method.HEAD;

public class LoadingAsyncTask extends AsyncTask<Void, Integer, JSONObject> {

    public static final String FIND_LOBBY_TOAST = "The chat is starting!";
    public static final String EXIT_LOBBY_TOAST = "You have exited the loading screen!";

    private QueueMatcher queueMatcher;
    private ProgressBar progressBar;
    private Activity currentActivty;
    private boolean findListener;
    private Activity currentActivity;
    private JSONObject jsonResponseContainer;

    LoadingAsyncTask(String currentUser, Activity currentActivity, ProgressBar progressBar, boolean findListener) {
        this.queueMatcher = new QueueMatcher(currentUser, currentActivity);
        this.currentActivty = currentActivity;
        this.progressBar = progressBar;
        this.currentActivity = currentActivity;
        this.progressBar.setVisibility(View.GONE);
        this.findListener = findListener;
    }

    @Override
    protected void onPreExecute() {
        this.progressBar.setVisibility(View.VISIBLE);
        this.progressBar.setMax(LobbyCheckerRunnable.TIME_LOOPING_MILLISECONDS);
        super.onPreExecute();
    }

    @Override
    protected JSONObject doInBackground(Void... voids) {

        if (findListener)
            queueMatcher.findListener();
        else
            queueMatcher.findSpeakers();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {

            if (findListener)
                queueMatcher.stopFindingListener();
            else
                queueMatcher.stopFindingSpeaker();

            e.printStackTrace();
            return null;
        }

        if (findListener)
            while (queueMatcher.isLoopCheckerAliveListener() && !isCancelled()) {
                publishProgress(queueMatcher.getLoopStateListener());

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    queueMatcher.stopFindingListener();
                    e.printStackTrace();
                    return null;
                }
            }
        else
            while (queueMatcher.isLoopCheckerAliveSpeaker() && !isCancelled()) {
                publishProgress(queueMatcher.getLoopStateSpeaker());

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    queueMatcher.stopFindingSpeaker();
                    e.printStackTrace();
                    return null;
                }
            }

        if (findListener)
            return queueMatcher.getListener();
        else
            return queueMatcher.getSpeakers();
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        progressBar.setProgress(values[0]);
    }

    @Override
    protected void onCancelled(JSONObject jsonObject) {
        Toast.makeText(this.currentActivty, EXIT_LOBBY_TOAST, Toast.LENGTH_SHORT).show();

        if (findListener)
            queueMatcher.stopFindingListener();
        else
            queueMatcher.stopFindingSpeaker();
    }

    @Override
    protected void onPostExecute(JSONObject jsonObject) {
        super.onPostExecute(jsonObject);
        this.progressBar.setVisibility(View.GONE);
        this.jsonResponseContainer = jsonObject;

        if (jsonObject == null) {
            Log.e("container : ", "null");
            if(!findListener)
            Toast.makeText(this.currentActivty, "There are no listeners in the queue", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this.currentActivty, "There are no persons to help in the queue", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(currentActivity, Main2Activity.class);
            currentActivity.startActivity(intent);
        } else {
            Log.e("container :", jsonObject.toString());
            Toast.makeText(this.currentActivty, FIND_LOBBY_TOAST, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(currentActivity, ChatActivity.class);
            currentActivity.startActivity(intent);

        }


    }


    public JSONObject getJsonResponseContainer() {
        return jsonResponseContainer;
    }
}
