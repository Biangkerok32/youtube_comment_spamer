import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Random;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import com.google.common.collect.Lists;

public class Main {

    private static String videoId = null;
    private static String channelId = null;
    private static YouTube youtube = null;
    private static List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube.force-ssl");
    private static String lastText = "";
    private static long lastPublishedDate = 0;
    private static long postedComments = 0;

    private static void initYouTube(String clientsSecrets, String googleApiPath) {
        if (youtube == null) {
            try {
                Credential credential = Auth.authorize(googleApiPath, clientsSecrets, scopes, "commentthreads");
                youtube = new YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential)
                        .setApplicationName("youtube-cmdline-commentthreads-sample").build();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void sendComment(String commentText) {
        try {
            CommentSnippet commentSnippet = new CommentSnippet();
            commentSnippet.setTextOriginal(commentText);

            Comment topLevelComment = new Comment();
            topLevelComment.setSnippet(commentSnippet);

            CommentThreadSnippet commentThreadSnippet = new CommentThreadSnippet();
            commentThreadSnippet.setChannelId(channelId);
            commentThreadSnippet.setTopLevelComment(topLevelComment);

            CommentThread commentThread = new CommentThread();
            commentThread.setSnippet(commentThreadSnippet);

            commentThreadSnippet.setVideoId(videoId);
            CommentThread videoCommentInsertResponse = youtube.commentThreads().insert("snippet", commentThread).execute();

            DateTime publishedAt = videoCommentInsertResponse.getSnippet().getTopLevelComment().getSnippet().getPublishedAt();
            lastPublishedDate = publishedAt.getValue();

            System.out.println("<----------------------------------->");
            System.out.println("Posted comments count: " + ++postedComments);
            System.out.println("Posted time: " + publishedAt.toString());
            System.out.println("Posted comment: " + commentText);
            System.out.println("URL: https://www.youtube.com/watch?v=" + videoCommentInsertResponse.getSnippet().getVideoId() + "&lc=" + videoCommentInsertResponse.getId());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getCommentText(long delayFromLastComment){
        try {
            CommentThreadListResponse videoCommentsListResponse = youtube.commentThreads()
                    .list("snippet").setMaxResults(100L).setVideoId(videoId).setTextFormat("plainText").execute();
            List<CommentThread> videoComments = videoCommentsListResponse.getItems();

            DateTime publishedAt = videoComments.get(0).getSnippet().getTopLevelComment().getSnippet().getPublishedAt();

            if(publishedAt.getValue() < lastPublishedDate + delayFromLastComment){
                return null;
            }

            String text = null;

            for(int i = videoComments.size() - 1; i > 0; i--){
                CommentThread videoComment = videoComments.get(videoComments.size() - 1);
                String videoCommentText = videoComment.getSnippet().getTopLevelComment().getSnippet().getTextDisplay();
                if (!lastText.equals(videoCommentText)) {
                    lastText = text = videoCommentText;
                    break;
                }
            }

            return text;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void main(String[] args) {
        try {
            Long delay = null;
            String clientsSecrets = null;
            String googleApiPath = null;
            Long lastPublishedCommentDelay = null;

            if(args.length == 1)
                try { delay = Long.valueOf(args[0]); } catch (Exception e) { delay = null; }

            if(args.length == 2){
                try { delay = Long.valueOf(args[0]); } catch (Exception e) { delay = null; }
                try { clientsSecrets = String.valueOf(args[1]); } catch (Exception e) { clientsSecrets = null; }
            }

            if(args.length == 3){
                try { delay = Long.valueOf(args[0]); } catch (Exception e) { delay = null; }
                try { clientsSecrets = String.valueOf(args[1]); } catch (Exception e) { clientsSecrets = null; }
                try { googleApiPath = String.valueOf(args[2]); } catch (Exception e) { googleApiPath = null; }
            }

            if(args.length == 4){
                try { delay = Long.valueOf(args[0]); } catch (Exception e) { delay = null; }
                try { clientsSecrets = String.valueOf(args[1]); } catch (Exception e) { clientsSecrets = null; }
                try { googleApiPath = String.valueOf(args[2]); } catch (Exception e) { googleApiPath = null; }
                try { lastPublishedCommentDelay = Long.valueOf(args[3]); } catch (Exception e) { lastPublishedCommentDelay = null; }
            }

            if(args.length == 5){
                try { delay = Long.valueOf(args[0]); } catch (Exception e) { delay = null; }
                try { clientsSecrets = String.valueOf(args[1]); } catch (Exception e) { clientsSecrets = null; }
                try { googleApiPath = String.valueOf(args[2]); } catch (Exception e) { googleApiPath = null; }
                try { lastPublishedCommentDelay = Long.valueOf(args[3]); } catch (Exception e) { lastPublishedCommentDelay = null; }
                try { videoId = String.valueOf(args[4]); } catch (Exception e) { videoId = null; }
            }

            if(args.length == 6){
                try { delay = Long.valueOf(args[0]); } catch (Exception e) { delay = null; }
                try { clientsSecrets = String.valueOf(args[1]); } catch (Exception e) { clientsSecrets = null; }
                try { googleApiPath = String.valueOf(args[2]); } catch (Exception e) { googleApiPath = null; }
                try { lastPublishedCommentDelay = Long.valueOf(args[3]); } catch (Exception e) { lastPublishedCommentDelay = null; }
                try { videoId = String.valueOf(args[4]); } catch (Exception e) { videoId = null; }
                try { channelId = String.valueOf(args[5]); } catch (Exception e) { channelId = null; }
            }

            if(delay == null)
                delay = getDelay();
            if(clientsSecrets == null)
                clientsSecrets = getClientSecretsPath();
            if(googleApiPath == null)
                googleApiPath = getCreedsPath();
            if(lastPublishedCommentDelay == null)
                lastPublishedCommentDelay = getLastPublishedCommentDelay();
            if(videoId == null)
                videoId = getVideoId();
            if(channelId == null)
                channelId = getChannelId();

            if(lastPublishedCommentDelay >= delay) { delay = lastPublishedCommentDelay + 1; }

            while (true){
                initYouTube(clientsSecrets, googleApiPath);

                String text = getCommentText(lastPublishedCommentDelay);

                if (text != null) {
                    sendComment(text);
                }

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static long getDelay() {

        long delay = 5000;

        System.out.print("Введите задержку в миллисекундах: ");
        try {
            BufferedReader bReader = new BufferedReader(new InputStreamReader(System.in));
            delay = Long.valueOf(bReader.readLine());
        } catch (Exception e) {
            System.out.print("Не удалось распознать введенный текст, будет использована задержка 5000ms");
        }

        return delay;
    }

    private static String getClientSecretsPath() throws Exception {

        String path = "";

        System.out.print("Введите путь к файлу clients_secrets.json: ");
        try {
            BufferedReader bReader = new BufferedReader(new InputStreamReader(System.in));
            path = bReader.readLine();
        } catch (Exception e) {
            throw new Exception("Не удалось распознать введенный текст");
        }

        return path;
    }

    private static String getCreedsPath() throws Exception {

        String path = "oauth-credentials";

        System.out.print("Введите путь к файлу для сохранения авторизационных данных Google API: ");
        try {
            BufferedReader bReader = new BufferedReader(new InputStreamReader(System.in));
            path = String.valueOf(bReader.readLine());
        } catch (Exception e) {
            throw new Exception("Не удалось распознать введенный текст, будет использована текущая дирректория");
        }

        return path;
    }

    private static Long getLastPublishedCommentDelay(){
        long delay = 60000;

        System.out.print("Введите задержку от последнего сообщения: ");
        try {
            BufferedReader bReader = new BufferedReader(new InputStreamReader(System.in));
            delay = Long.valueOf(bReader.readLine());
        } catch (Exception e) {
            System.out.print("Не удалось распознать введенный текст, будет использована задержка 60 секунд");
        }

        return delay;
    }

    private static String getVideoId() throws Exception {
        String path = null;

        System.out.print("Введите video ID: ");
        try {
            BufferedReader bReader = new BufferedReader(new InputStreamReader(System.in));
            path = String.valueOf(bReader.readLine());
        } catch (Exception e) {
            throw new Exception("Не удалось распознать введенный текст");
        }

        return path;
    }

    private static String getChannelId() throws Exception {
        String path = null;

        System.out.print("Введите channel ID: ");
        try {
            BufferedReader bReader = new BufferedReader(new InputStreamReader(System.in));
            path = String.valueOf(bReader.readLine());
        } catch (Exception e) {
            throw new Exception("Не удалось распознать введенный текст");
        }

        return path;
    }

}
