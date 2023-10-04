༼ つ ◕_◕ ༽つ SPLASHSCREEN
1. Create a new empty activity, name it 'SplashScreenActivity'
2. Select 'Launcher Activity'
3. Remove Launcher from MainActivity in AndroidManifest
3. Import SplashScreen image (gif image)
4. Import dependency for gif
5. Use Handler to show SplashScreen for 3 seconds before going to MainActivity

༼ つ ◕_◕ ༽つ APP ICON
1. Create in mipmap, and change it in AndroidManifest

༼ つ ◕_◕ ༽つ THEMES
1. Set default font, status bar color, etc
2. Set to only portrait mode in MainActivity

༼ つ ◕_◕ ༽つ CUSTOMACTIONBAR
1. Create a custom theme for MainActivity (Action Bar)
2. Set the theme in AndroidManifest
3. Create a layout for the custom actionbar called 'actionbar_custom_layout'
4. Add code in MainActivity to set up custom action bar [setCustomActionBar()]

༼ つ ◕_◕ ༽つ TABLAYOUT & VIEWPAGER2 & FRAGMENTS
1. Add Tablayout and ViewPager2 in activity_main
2. Create a new activity called 'ViewPagerAdapter' that extends FragmentStateAdapter
   - Helps to handle the fragments and ViewPager2
3. In ViewPagerAdapter, create 2 ArrayLists, one for the fragments, and one for the fragment's titles
4. Create a new function [addFragment()] that takes in a fragment and title, add these into the 2 ArrayLists
5. Create a constructor matching super??
6. Change the createFragment() to return fragmentList.get(position)
7. Change the getItemCount() to return fragmentList.size()
8. Create a new nullable function [getPageTitle()] to return fragment title
9. Now back to MainActivity, create & set adapter for viewPager2 [setViewPager()], also use TabLayoutMediator to connect Tablayout with ViewPager2
10. Now create the fragments and add them in this function using [addFragment()] from ViewPagerAdapter

༼ つ ◕_◕ ༽つ RETRIEVING SONGS + PERMISSIONS + APPDATA
1. Add permissions in AndroidManifest
2. In MainActivity, request for permissions
3. Create a 'Song' Data Model, this will be what each 'Song' should have
4. Create a new activity 'MusicManager', this will retrieve the mp3 files 
5. In fragment_song, add in the buttons and recyclerview 
6. Create a layout for each 'Song' called 'item_song' 
7. Make SongFragment implements SongAdapter.ItemClickListener
8. Create a singleton (can be accessed from anywhere in the app) called 'AppData'
9. Fetch the mp3 files at SplashScreenActivity, and store it in the appData
10. Fetch the stored mp3 files from AppData, and display it in SongsFragment

༼ つ ◕_◕ ༽つ PLAYLISTS
1. Create a 'Playlist' Data Model
2. Create a new activity 'DatabaseHelper'
3. Create a new adapter 'PlaylistAdapter'
4. Create a new layout 'item_playlist'
5. For creating a new playlist, first make a layout for the dialog 'dialog_add_playlist'
6. Create a new bottom layout popup 'layout_bottom_playlist_popup'
6. Set up delete of playlist
7. Create a new activity 'PlaylistFolderActivity'
8. Pass in the playlist id and show the title and album image

༼ つ ◕_◕ ༽つ PLAYING MUSIC WITH MEDIAPLAYERSERVICE
1. Create a MediaPlayerService class that extends Service and Implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener
2. Remember to add in AndroidManifest
3. Also for Scoped Storage on Android 10 and above 
4. Add android:requestLegacyExternalStorage="true" in AndroidManifest.xml