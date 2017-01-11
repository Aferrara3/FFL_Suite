import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class league {

	int leagueId, seasonId, numPlayers;
	static int weekCompleted;
	double[][] PF, PA, outcome;
	String[] playerNames, players;
	boolean[][] wins;
	
	public league(int leagueId, int seasonId) throws IOException {
		   this.leagueId= leagueId;
		   this.seasonId = seasonId;
		   if(seasonId == 2016){ this.weekCompleted = Espn_Soup.weekCompleted; }else{ weekCompleted = 17; }

		   String standingsUrl = "http://games.espn.com/ffl/standings?leagueId="+leagueId+"&seasonId="+seasonId;
		   String doc = Jsoup.connect(standingsUrl).get().toString();
		   String playersTable = doc.split("class=\"games-fullcol games-fullcol-extramargin\"")[1].split("id=\"xstandTbl_div0\"")[0];
		   players = playersTable.split("teamId=");
		   numPlayers = (players.length); //Really numPLayers + 1, but functionally num players for indexing
		   playerNames = new String[numPlayers];
		   PF = new double[numPlayers][16];
		   PA = new double[numPlayers][16];
		   outcome = new double[numPlayers][16]; //Players are rows, weeks are cols - same as FF Chart Generator xlsx
		   wins = new boolean[numPlayers][16];
		 //Initialize optimized shit to be kept track of here
			//optimizedPF, PA, outcome, wins, weekProcessed (same all as above?)
		   
		   populateNames();
	}
	
	/*Populate Player Names*/
	private void populateNames(){
		for (int i = 1; i<numPlayers ; i++){
			int playerId = Integer.parseInt(players[i].split("&")[0]);
			String playerName = players[i].split("\">")[1].split("</a")[0];
			playerNames[playerId] = playerName; 
			//System.out.println(playerId+": "+playerName);
		}
		//System.out.println("Finished populating player names");
	}
	
	protected void populateFull() throws IOException{
		populateStatsForRange(1, weekCompleted);
	}
	
	protected void populateStatsForRange(int startWeek, int endWeek) throws IOException{
		//Populate (PF+PA+outcome+wins)/week : 1-13
		for(int i = 1; i<=endWeek; i++){
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
		if (endWeek > 13){
			for(int i = 14; i <= endWeek; i++){
				//Insert parsing code here from scorecard
			}
		}
	}
	
	protected void printAllStats(){
		printStatsForRange(1,weekCompleted);
	}
	
	protected void printStatsForRange(int startWeek, int endWeek){
		//Data Here, Time to Process and Graph and Shit
		for(int i=1; i<numPlayers; i++){
			System.out.print(i + ": " + playerNames[i] + "\n");
			System.out.print("PF: "); 		printArray(PF[i], startWeek, endWeek);
			System.out.print("PA: "); 		printArray(PA[i], startWeek, endWeek);
			System.out.print("outcome: "); 	printArray(outcome[i], startWeek, endWeek);
			System.out.print("Wins: ");		printArray(wins[i], startWeek, endWeek);
			System.out.println("\n");
		}
	}
	
	double[][] op_PF, op_PA, op_outcome;
	boolean[][] op_wins;
	int teamId, week;
	ArrayList<String> startersPos;
	protected void populateOptimized(int teamId, int week) throws IOException{
		String url = "http://games.espn.com/ffl/boxscorescoring?leagueId="+leagueId+"&teamId="+teamId+"&scoringPeriodId="+week+"&seasonId="+seasonId+"&view=scoringperiod&version=scoring";
		startersPos = new ArrayList<String>();
		//needs teamId and week as loop vars
		/*
		 * Identify 2 team ids
		 * Record all players on roster position/points
		 * Iterate through lineup positions available for league
		 * 	Identify highest available point for that position
		 *  Add points for that player to total and remove player as option
		 *  	Do so by setting thei points to 0 => then if all else negative this will win as equivalent of not playing anyone
		 *  Loop through PF and op_PF to see who had the most times with optimal lineups
		 *  
		 *  !*!*!*!*! BEWARE BOUNDS ISSUES WHEN SOMEONE DOESN"T HAVE A FULL BENCH !*!*!*!*!
		 * */
		Document doc = Jsoup.connect(url).get();
		Elements players = doc.select("tr.pncPlayerRow");
		String[] positions = new String[players.size()];
		double[] scores = new double[players.size()];
		int k = 0;
		boolean firstSet = true;
		boolean team2 = false;
		int[] teamSize = {0, 0};
		for(Element player : players ){
			String text = player.text();
			String posPlayed = text.split(" ")[0];
			String posActual = text.split(",|\\D/ST",2)[1].split(" ")[1].split(" ")[0];
			
			//Store posActual and score in arrays here in order
			positions[k] = posActual;	 
			scores[k] = Double.parseDouble(text.split(" ")[text.split(" ").length-1]);;
			System.out.println(text);

			if(firstSet){
				if(posPlayed.equals("Bench")){ firstSet = false;}
				else startersPos.add(posPlayed);
			}
			if(!firstSet){
				if (team2 == false && !posPlayed.equals("Bench")){
					team2 = true;
				}
			}
			if(!team2) teamSize[0] = teamSize[0]+1;
			else teamSize[1] = teamSize[1]+1;
		}
		printArray(teamSize,0,1);
		double[] startersScores = new double[startersPos.size()];
		Arrays.fill(startersScores, 0);
			//Loop through positions and players every time sounds shitty (but easier)
			//Store starters values on first run and just compare bench?
				// Would have to compare bench until no more changes made, as multiple rounds may be needed for FLEX - min 2 runs to verify 1

			/* Brute force approach */ 
			for(int i=0; i<positions.length; i++){
	            double pScore = scores[i];
	            String pPos = positions[i];
	            
	            for(int j=0; j<startersPos.size(); j++){
	                String currPos = startersPos.get(j);
	                double currScore = startersScores[j];
	                if(pPos.equals(currPos) || (pPos.matches("A|B|D")&&currPos.equals("FLEX"))){
	                    if(pScore > currScore){
	                        double temp = currScore;
	                        currScore = pScore;
	                        pScore = temp;
	                        startersScores[j] = currScore;
	                    }
	                }
	            }
	            printArray(startersScores,0,startersScores.length);
			}
			/* End brute force */
			
	}
	
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
	
	private static void printArray(boolean[] anArray, int start, int end) {
	    for (int i = start; i <= end; i++) {
	       if (i > start) {
	          System.out.print(", ");
	       }
	       System.out.print(anArray[i]);
	    }
	}
	private static void printArray(double[] anArray, int start, int end) {
	    for (int i = start; i <= end; i++) {
	       if (i > start) {
	          System.out.print(", ");
	       }
	       System.out.print(anArray[i]);
	    }
	    System.out.print("\n");
	}
	private static void printArray(int[] anArray, int start, int end) {
	    for (int i = start; i <= end; i++) {
	       if (i > start) {
	          System.out.print(", ");
	       }
	       System.out.print(anArray[i]);
	    }
	    System.out.print("\n");
	}
}