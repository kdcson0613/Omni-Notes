package it.feio.android.omninotes;

import org.junit.*;

public class NavigationDrawerFragmentTest {

    private NavigationDrawerFragment eventDraw = new NavigationDrawerFragment();

    @Before
    public void Setup(){
    }

    @After
    public void tearDown(){
        eventDraw = null;
    }

    @Test
    public void testFunc(){
        try {
            eventDraw.onEventHandler(null , null, null, null);
        } catch ( NullPointerException e) {
            Assert.assertEquals("Null Exception Error",e.toString(), "Null object");
        }
    }
}