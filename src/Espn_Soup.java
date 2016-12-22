import org.jsoup.*;
import org.jsoup.Connection.Method;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Espn_Soup {
	static int weekCompleted = 3;
	public static void main(String[] args) throws IOException, InterruptedException {
		
		/* START SECTION ESPN STORING COOKIES FROM LOGIN */
		/*String url = "https://ha.registerdisney.go.com/jgc/v5/client/ESPN-ESPNCOM-PROD/guest/login?langPref=en-US";
        URL obj = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
        conn.setReadTimeout(5000);
        conn.addRequestProperty("Host","ha.registerdisney.go.com");
		conn.addRequestProperty("Connection","keep-alive");
		conn.addRequestProperty("Content-Length","62");
		conn.addRequestProperty("pragma","no-cache");
		conn.addRequestProperty("correlation-id","a414fdcd-c461-4271-ba2e-8874e8acbfed");
		conn.addRequestProperty("authorization","APIKEY 7x2yLpIHWiNkwOKb3c0IP633AjNs3100/vScPqoF2E8l2iGro4D/0JjOX/SnXgrWQ8+U5w2bVO0jegAun7pV9z4=");
		conn.addRequestProperty("content-type","application/json");
		conn.addRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36");	    	
		conn.addRequestProperty("cache-control","no-cache");
		conn.addRequestProperty("conversation-id","d07a42a1-a731-4754-99b6-cea689eaa669, a4dc5f11-6c67-48c6-a984-0bdb13c9e9e7");
		conn.addRequestProperty("Origin","https://cdn.registerdisney.go.com");
		conn.addRequestProperty("expires","-1");
		conn.addRequestProperty("Accept","*");
		conn.addRequestProperty("Referer","https://cdn.registerdisney.go.com/v2/ESPN-FANTASYLM-PROD/en-US?include=config,l10n,js,html&scheme=http&postMessageOrigin=http%3A%2F%2Fwww.espn.com%2Ffantasy%2Ffootball%2F&cookieDomain=www.espn.com&config=PROD&logLevel=INFO&topHost=www.espn.com&cssOverride=https%3A%2F%2Fsecure.espncdn.com%2Fcombiner%2Fc%3Fcss%3Ddisneyid%2Fcore.css%2Cdisneyid%2Ffantasy.css&responderPage=https%3A%2F%2Fwww.espn.com%2Flogin%2Fresponder%2Findex.html&buildId=1582710fc89");
		conn.addRequestProperty("Accept-Encoding","gzip, deflate, br");
		conn.addRequestProperty("Accept-Language","en-US,en;q=0.8");
        conn.setDoOutput(true);

        OutputStreamWriter w = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");

        w.write("{\"loginValue\":\"EMAIL\",\"password\":\"PASSWORD\"}");
        w.close();

        System.out.println("Request URL ... " + url);

        int status = conn.getResponseCode();

        System.out.println("Response Code ... " + status);

        BufferedReader in = new BufferedReader(new InputStreamReader(
                conn.getInputStream()));
        String inputLine;
        StringBuffer html = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            html.append(inputLine);
        }

        in.close();
        CookieManager cm = new CookieManager();
        cm.storeCookies(conn);
        conn.disconnect();
        System.out.println("URL Content... \n" + html.toString());
        System.out.println("Done");
    
    Document doc = Jsoup.connect("http://www.espn.com/fantasy/football/")
  	      .header("Cookies",cm.toString())
  	      .get();
    
    System.out.println("");*/
    /* END SECTION GETTING COOKIES FROM ESPNFFL LOGIN */
    
    
    /*Take a break to work on parsing once you are logged in. Fuck this cookies shit.*/
        int leagueId = 1283828;
        int seasonId = 2016;
		String standingsUrl = "http://games.espn.com/ffl/standings?leagueId="+leagueId+"&seasonId="+seasonId;
		String doc = Jsoup.connect(standingsUrl).get().toString();
		String playersTable = doc.split("class=\"games-fullcol games-fullcol-extramargin\"")[1].split("id=\"xstandTbl_div0\"")[0];
		String[] players = playersTable.split("teamId=");
		int numPlayers = (players.length); //Really numPLayers + 1, but functionally num players for indexing
		String[] playerNames = new String[numPlayers];
		double[][] PF = new double[numPlayers][16];
		double[][] PA = new double[numPlayers][16];
		double[][] outcome = new double[numPlayers][16]; //Players are rows, weeks are cols - same as FF Chart Generator xlsx
		boolean[][] wins = new boolean[numPlayers][16];
		//int[][] weekProcessed = new int[numPlayers][16];
		
		//Optimized shit to be kept track of here
			//optimized: PF, PA, outcome, wins, weekProcessed (same all as above)
		
		//Populate Player Names
		for (int i = 1; i<numPlayers ; i++){
			int playerId = Integer.parseInt(players[i].split("&")[0]);
			String playerName = players[i].split("\">")[1].split("</a")[0];
			playerNames[playerId] = playerName; 
			//System.out.println(playerId+": "+playerName);
		}
		//System.out.println("Finished populating player names");
		
		//Populate (PF+PA+outcome+wins)/week : 1-13
		for(int i = 1; i<=weekCompleted; i++){
			String weekUrl = "http://games.espn.com/ffl/scoreboard?leagueId="+leagueId+"&matchupPeriodId="+i;
        	Document week = Jsoup.connect(weekUrl).get();
			Element content = week.getElementById("scoreboardMatchups");
			Elements matchups = content.getElementsByClass("matchup");
			//System.out.println("TEST :" + content.toString());
			for (Element matchup : matchups) {
			  String[] teams_temp = matchup.toString().split("<tr",4);
			  String[] teams = {teams_temp[1], teams_temp[2]};
			  int t1id = -1;
			  double t1Score = 0;
			  
			  int id=0; double score=0;	
			  for(String team : teams){
				team = team.split("</tr>")[0];
				id  = Integer.parseInt(team.split("_")[1]);
				boolean won = team.contains("winning");
				score = Double.parseDouble(team.split("score\" title=")[1].split("\"")[1]);
				//System.out.println(id+": "+ score);				
				PF[id][i] = score;
				wins[id][i] = won;
				if(t1id == -1){ t1id = id; t1Score = score; }
			  }
			  outcome[id][i] = (double) Math.round((score - t1Score) * 10) / 10;
			  outcome[t1id][i] = (double) Math.round((t1Score - score) * 10) / 10;
			  PA[t1id][i] = score;
			  PA[id][i] = t1Score;  
			  //System.out.println("ENDMATCHUP\n"); 
			}
			//TimeUnit.SECONDS.sleep(2);//Wait 2s to make more realistic as human clicks
			//System.out.println("END WEEK " + i);
		}
		//Playoffs have to be done by specific pages for each week
		if (weekCompleted > 13){
			for(int i = 14; i <= weekCompleted; i++){
				//Insert parsing code here from scorecard
			}
		}
		
		//Data Here, Time to Process and Graph and Shit
		for(int i=1; i<numPlayers; i++){
			System.out.print(i + ": " + playerNames[i] + "\n");
			System.out.print("PF: "); 		printArray(PF[i]);
			System.out.print("PA: "); 		printArray(PA[i]);
			System.out.print("outcome: "); 	printArray(outcome[i]);
			System.out.print("Wins: ");		printArray(wins[i]);
			System.out.println("\n");
		}
    }

	private static void printArray(boolean[] anArray) {
	      for (int i = 1; i <= weekCompleted; i++) {
	         if (i > 1) {
	            System.out.print(", ");
	         }
	         System.out.print(anArray[i]);
	      }
	}
	private static void printArray(double[] anArray) {
	      for (int i = 1; i <= weekCompleted; i++) {
	         if (i > 1) {
	            System.out.print(", ");
	         }
	         System.out.print(anArray[i]);
	      }
	      System.out.print("\n");
	}
}

/*****************************************************
					To Do Items
******************************************************
	 - Setup github repo for versioning safety
 	 - Modularize into relevant functions
 		- Param weeks range
 	- Optimized lineups
 		- Week + playerid params
	- Front end
 	- Pushing to db
 		- db design
 	- Scoreboard parsing for weeks 14-17
 	- Name parsing with team names
 		- Division separation for playoff odds
 	- Tie in the cookies
 	- Playoff odds calculations
 	- CSV Export
******************************************************/


