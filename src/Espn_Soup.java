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
	public static int weekCompleted = 3;
	public static void main(String[] args) throws IOException, InterruptedException {
		
		league my_league = new league(1283828, 2016);
		//my_league.populateFull();
		my_league.populateStatsForRange(1,2);
		my_league.printStatsForRange(1,2);
		
    }
}

/*****************************************************
					To Do Items
******************************************************
	 - Setup github repo for versioning safety - DONE
 	 - Modularize into relevant functions - DONE
 		- Param weeks range - DONE
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


