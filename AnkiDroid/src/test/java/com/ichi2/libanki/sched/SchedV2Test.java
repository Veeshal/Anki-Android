/*
 Copyright (c) 2020 David Allison <davidallisongithub@gmail.com>

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU General Public License as published by the Free Software
 Foundation; either version 3 of the License, or (at your option) any later
 version.

 This program is distributed in the hope that it will be useful, but WITHOUT ANY
 WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 PARTICULAR PURPOSE. See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with
 this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ichi2.libanki.sched;

import android.database.Cursor;

import com.ichi2.anki.RobolectricTest;
import com.ichi2.anki.exception.ConfirmModSchemaException;
import com.ichi2.libanki.Card;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Consts;
import com.ichi2.libanki.Deck;
import com.ichi2.libanki.DeckConfig;
import com.ichi2.libanki.Model;
import com.ichi2.libanki.Models;
import com.ichi2.libanki.Note;
import com.ichi2.libanki.Utils;
import com.ichi2.testutils.MockTime;
import com.ichi2.utils.JSONArray;
import com.ichi2.utils.JSONObject;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static com.ichi2.libanki.Consts.CARD_TYPE_LRN;
import static com.ichi2.libanki.Consts.CARD_TYPE_NEW;
import static com.ichi2.libanki.Consts.CARD_TYPE_RELEARNING;
import static com.ichi2.libanki.Consts.CARD_TYPE_REV;
import static com.ichi2.libanki.Consts.LEECH_SUSPEND;
import static com.ichi2.libanki.Consts.QUEUE_TYPE_DAY_LEARN_RELEARN;
import static com.ichi2.libanki.Consts.QUEUE_TYPE_LRN;
import static com.ichi2.libanki.Consts.QUEUE_TYPE_MANUALLY_BURIED;
import static com.ichi2.libanki.Consts.QUEUE_TYPE_NEW;
import static com.ichi2.libanki.Consts.QUEUE_TYPE_REV;
import static com.ichi2.libanki.Consts.QUEUE_TYPE_SIBLING_BURIED;
import static com.ichi2.libanki.Consts.STARTING_FACTOR;
import static com.ichi2.libanki.DecksTest.TEST_DECKS;
import static com.ichi2.libanki.Utils.intTime;
import static com.ichi2.testutils.AnkiAssert.checkRevIvl;
import static com.ichi2.testutils.AnkiAssert.without_unicode_isolation;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;
import static org.junit.platform.commons.util.CollectionUtils.getOnlyElement;

@RunWith(AndroidJUnit4.class)
public class SchedV2Test extends RobolectricTest {

    protected static List<DeckDueTreeNode> expectedTree(Collection col, boolean addRev) {
        AbstractSched sched = col.getSched();
        DeckDueTreeNode caz = new DeckDueTreeNode(col, "cmxieunwoogyxsctnjmv::abcdefgh::ZYXW", 1, 0, 0, 0);
        caz.setChildren(new ArrayList<>(), addRev);
        DeckDueTreeNode ca = new DeckDueTreeNode(col, "cmxieunwoogyxsctnjmv::abcdefgh", 1, 0, 0, 0);
        ca.setChildren(Arrays.asList(caz), addRev);
        DeckDueTreeNode ci = new DeckDueTreeNode(col, "cmxieunwoogyxsctnjmv::INSBGDS", 1, 0, 0, 0);
        ci.setChildren(new ArrayList<>(), addRev);
        DeckDueTreeNode c = new DeckDueTreeNode(col, "cmxieunwoogyxsctnjmv", 1, 0, 0, 0);
        c.setChildren(Arrays.asList(ci, ca), addRev);
        DeckDueTreeNode defaul = new DeckDueTreeNode(col, "Default", 1, 0, 0, 0);
        defaul.setChildren(new ArrayList<>(), addRev);
        DeckDueTreeNode s = new DeckDueTreeNode(col, "scxipjiyozczaaczoawo", 1, 0, 0, 0);
        s.setChildren(new ArrayList<>(), addRev);
        List<DeckDueTreeNode> expected = Arrays.asList(defaul, c, s); // Default is first, because start by an Upper case
        return expected;
    }


    /**
     * Reported by /u/CarelessSecretary9 on reddit:
     */
    @Test
    public void filteredDeckSchedulingOptionsRegressionTest() {
        getCol().setCrt(1587852900L);
        //30 minutes learn ahead. required as we have 20m delay
        getCol().getConf().put("collapseTime", 1800);

        long homeDeckId = addDeck("Poorretention");

        DeckConfig homeDeckConf = getCol().getDecks().confForDid(homeDeckId);
        JSONObject lapse = homeDeckConf.getJSONObject("lapse");

        lapse.put("minInt", 2);
        lapse.put("mult", 0.7d);
        lapse.put("delays", new JSONArray("[20]"));

        ensureLapseMatchesSppliedAnkiDesktopConfig(lapse);

        getCol().flush();

        long dynId = addDynamicDeck("Dyn");

        /*
        >>> pp(self.reviewer.card)
        {'data': '', 'did': 1587939535230, 'due': 0, 'factor': 1300, 'flags': 0, 'id': 1510928829863, 'ivl': 25,
        'lapses': 5, 'left': 1004, 'mod': 1587921512, 'nid': 1510928805161, 'odid': 1587920944107,
        'odue': 0, 'ord': 0, 'queue': 2, 'reps': 22, 'type': 2, 'usn': -1}

         */
        Note n = addNoteUsingBasicModel("Hello", "World");
        Card c = getOnlyElement(n.cards());
        c.setType(Consts.CARD_TYPE_REV);
        c.setQueue(Consts.QUEUE_TYPE_REV);
        c.setIvl(25);
        c.setDue(0);
        c.setLapses(5);
        c.setFactor(1300);
        c.setLeft(1004);
        c.setODid(homeDeckId);
        c.setDid(dynId);
        c.flush();

        SchedV2 v2 = new SchedV2(getCol(), new MockTime(1587928085001L));

        Card schedCard = v2.getCard();
        assertThat(schedCard, Matchers.notNullValue());
        v2.answerCard(schedCard, Consts.BUTTON_ONE);
        assertThat("The lapsed card should now be counted as lrn", v2.mLrnCount, is(1));
        Card after = v2.getCard();
        assertThat("A card should be returned ", after, Matchers.notNullValue());

        /* Data from Anki - pp(self.reviewer.card)
        {'data': '', 'did': 1587939535230, 'due': 1587941137, 'factor': 1300,
        'flags': 0, 'id': 1510928829863, 'ivl': 17, 'lapses': 6, 'left': 1001,
        'mod': 1587939720, 'nid': 1510928805161, 'odid': 1587920944107, 'odue': 0,
        'ord': 0, 'queue': 1, 'reps': 23, 'type': 3, 'usn': -1}
         */
        assertThat(after.getType(), is(Consts.CARD_TYPE_RELEARNING));
        assertThat(after.getQueue(), is(Consts.QUEUE_TYPE_LRN));
        assertThat(after.getLeft(), is(1001));
        assertThat("ivl is reduced by 70%", after.getIvl(), is(17));
        assertThat("One lapse is added", after.getLapses(), is(6));

        assertThat(v2.answerButtons(after), is(4));

        long one = v2.nextIvl(after, Consts.BUTTON_ONE);
        long two = v2.nextIvl(after, Consts.BUTTON_TWO);
        long three = v2.nextIvl(after, Consts.BUTTON_THREE);
        long four = v2.nextIvl(after, Consts.BUTTON_FOUR);

        assertThat("Again should pick the current step", one, is(1200L));      // 20 mins
        assertThat("Repeating single step - 20 minutes * 1.5", two, is(1800L));      // 30 mins
        assertThat("Good should take the reduced interval (25 * 0.7)", three, is(1468800L)); // 17 days
        assertThat("Easy should have a bonus day over good", four, is(1555200L));  // 18 days
    }


    private void ensureLapseMatchesSppliedAnkiDesktopConfig(JSONObject lapse) {
        assertThat(lapse.getInt("minInt"), is(2));
        assertThat(lapse.getDouble("mult"), is(0.7d));
        assertThat(lapse.getJSONArray("delays").length(), is(1));
        assertThat(lapse.getJSONArray("delays").getDouble(0), is(20.));

    }


    @Test
    public void ensureDeckTree() throws Exception {
        for (String deckName : TEST_DECKS) {
            addDeck(deckName);
        }
        AbstractSched sched = getCol().getSched();
        List<DeckDueTreeNode> tree = sched.deckDueTree();
        Assert.assertEquals("Tree has not the expected structure", expectedTree(getCol(), true), tree);
    }


    /*****************
     ** autogenerated from https://github.com/ankitects/anki/blob/2c73dcb2e547c44d9e02c20a00f3c52419dc277b/pylib/tests/test_cards.py
     *****************/
    private MockTime mTime = new MockTime(1596540139000L, 10);

    public Collection getColV2() throws Exception {
        Collection col = getCol();
        changeSchedulerVer(col, 2);
        return col;
    }

    private void changeSchedulerVer(Collection col, int ver) throws ConfirmModSchemaException {
        col.changeSchedulerVer(ver);
        col.setCrt(1596540138L);
        col.replaceSchedulerForTests(mTime);
    }

    private double now() {
        return mTime.now();
    }

    private long intTime() {
        return mTime.intTime();
    }

    @Test
    public void test_clock() throws Exception {
        Collection col = getColV2();
        if ((col.getSched().getDayCutoff() - mTime.intTime()) < 10 * 60) {
            throw new Exception("Unit tests will fail around the day rollover.");
        }
    }


    @Test
    public void test_basics() throws Exception {
        Collection col = getColV2();
        col.reset();
        assertNull(col.getSched().getCard());
    }


    @Test
    public void test_new_v2() throws Exception {
        Collection col = getColV2();
        col.reset();
        // assertEquals(0, col.getSched().newCount);TODO: newCount getter
        // add a note
        Note note = col.newNote();
        note.setItem("Front", "one");
        note.setItem("Back", "two");
        col.addNote(note);
        col.reset();
        // assertEquals(1, col.getSched().newCount);TODO: newCount getter
        // fetch it
        Card c = col.getSched().getCard();
        assertNotNull(c);
        assertEquals(QUEUE_TYPE_NEW, c.getQueue());
        assertEquals(CARD_TYPE_NEW, c.getType());
        // if we answer it, it should become a learn card
        long t = intTime();
        col.getSched().answerCard(c, 1);
        assertEquals(QUEUE_TYPE_LRN, c.getQueue());
        assertEquals(CARD_TYPE_LRN, c.getType());
        assertTrue(c.getDue() >= t);

        // disabled for now, as the learn fudging makes this randomly fail
        // // the default order should ensure siblings are not seen together, and
        // // should show all cards
        // Model m = col.getModels().current(); Models mm = col.getModels()
        // JSONObject t = mm.newTemplate("Reverse")
        // t['qfmt'] = "{{Back}}"
        // t['afmt'] = "{{Front}}"
        // mm.addTemplateModChanged(m, t)
        // mm.save(m)
        // note = col.newNote()
        // note['Front'] = u"2"; note['Back'] = u"2"
        // col.addNote(note)
        // note = col.newNote()
        // note['Front'] = u"3"; note['Back'] = u"3"
        // col.addNote(note)
        // col.reset()
        // qs = ("2", "3", "2", "3")
        // for (int n = 0; n < 4; n++) {
        //     c = col.getSched().getCard()
        //     assertTrue(qs[n] in c.q())
        //     col.getSched().answerCard(c, 2)
        // }
    }


    @Test
    public void test_newLimits_V2() throws Exception {
        Collection col = getColV2();
        // add some notes
        long deck2 = col.getDecks().id("Default::foo");
        for (int i = 0; i < 30; i++) {
            Note note = col.newNote();
            note.setItem("Front", Integer.toString(i));
            if (i > 4) {
                note.model().put("did", deck2);
            }
            col.addNote(note);
        }
        // give the child deck a different configuration
        long c2 = col.getDecks().confId("new conf");
        col.getDecks().setConf(col.getDecks().get(deck2), c2);
        col.reset();
        // both confs have defaulted to a limit of 20
        // assertEquals(20, col.getSched().newCount);TODO: newCount getter
        // first card we get comes from parent
        Card c = col.getSched().getCard();
        assertEquals(1, c.getDid());
        // limit the parent to 10 cards, meaning we get 10 in total
        DeckConfig conf1 = col.getDecks().confForDid(1);
        conf1.getJSONObject("new").put("perDay", 10);
        col.getDecks().save(conf1);
        col.reset();
        // assertEquals(10, col.getSched().newCount);TODO: newCount getter
        // if we limit child to 4, we should get 9
        DeckConfig conf2 = col.getDecks().confForDid(deck2);
        conf2.getJSONObject("new").put("perDay", 4);
        col.getDecks().save(conf2);
        col.reset();
        //assertEquals(9, col.getSched().newCount);TODO: newCount getter
    }


    @Test
    public void test_newBoxes_v2() throws Exception {
        Collection col = getColV2();
        Note note = col.newNote();
        note.setItem("Front", "one");
        col.addNote(note);
        col.reset();
        Card c = col.getSched().getCard();
        DeckConfig conf = col.getSched()._cardConf(c);
        conf.getJSONObject("new").put("delays", new JSONArray(new double[] {1, 2, 3, 4, 5}));
        col.getDecks().save(conf);
        col.getSched().answerCard(c, 2);
        // should handle gracefully
        conf.getJSONObject("new").put("delays", new JSONArray(new double[] {1}));
        col.getDecks().save(conf);
        col.getSched().answerCard(c, 2);
    }


    @Test
    public void test_learnV2() throws Exception {
        Collection col = getColV2();
        // add a note
        Note note = col.newNote();
        note.setItem("Front", "one");
        note.setItem("Back", "two");
        col.addNote(note);
        // set as a learn card and rebuild queues
        col.getDb().execute("update cards set queue=0, type=0");
        col.reset();
        // sched.getCard should return it, since it's due in the past
        Card c = col.getSched().getCard();
        assertNotNull(c);
        DeckConfig conf = col.getSched()._cardConf(c);
        conf.getJSONObject("new").put("delays", new JSONArray(new double[] {0.5, 3, 10}));
        col.getDecks().save(conf);
        // fail it
        col.getSched().answerCard(c, 1);
        // it should have three reps left to graduation
        assertEquals(3, c.getLeft() % 1000);
        assertEquals(3, c.getLeft() / 1000);
        // it should be due in 30 seconds
        long t = Math.round(c.getDue() - now());
        assertTrue(t >= 25 && t <= 40);
        // pass it once
        col.getSched().answerCard(c, 3);
        // it should be due in 3 minutes
        double dueIn = c.getDue() - now();
        assertTrue(178 <= dueIn && dueIn <= 180 * 1.25);
        assertEquals(2, c.getLeft() % 1000);
        assertEquals(2, c.getLeft() / 1000);
        // check log is accurate
        Cursor log = col.getDb().getDatabase().query("select * from revlog order by id desc");
        assertTrue(log.moveToFirst());
        assertEquals(3, log.getInt(3));
        assertEquals(-180, log.getInt(4));
        assertEquals(-30, log.getInt(5));
        // pass again
        col.getSched().answerCard(c, 3);
        // it should be due in 10 minutes
        dueIn = c.getDue() - now();
        assertTrue(599 <= dueIn && dueIn <= 600 * 1.25);
        assertEquals(1, c.getLeft() % 1000);
        assertEquals(1, c.getLeft() / 1000);
        // the next pass should graduate the card
        assertEquals(QUEUE_TYPE_LRN, c.getQueue());
        assertEquals(CARD_TYPE_LRN, c.getType());
        col.getSched().answerCard(c, 3);
        assertEquals(QUEUE_TYPE_REV, c.getQueue());
        assertEquals(CARD_TYPE_REV, c.getType());
        // should be due tomorrow, with an interval of 1
        assertEquals(col.getSched().getToday() + 1, c.getDue());
        assertEquals(1, c.getIvl());
        // or normal removal
        c.setType(CARD_TYPE_NEW);
        c.setQueue(QUEUE_TYPE_LRN);
        col.getSched().answerCard(c, 4);
        assertEquals(CARD_TYPE_REV, c.getType());
        assertEquals(QUEUE_TYPE_REV, c.getQueue());
        assertTrue(checkRevIvl(col, c, 4));
        // revlog should have been updated each time
        assertEquals(5, col.getDb().queryScalar("select count() from revlog where type = 0"));
    }


    @Test
    public void test_relearn() throws Exception {
        Collection col = getColV2();
        Note note = col.newNote();
        note.setItem("Front", "one");
        col.addNote(note);
        Card c = note.cards().get(0);
        c.setIvl(100);
        c.setDue(col.getSched().getToday());
        c.setQueue(CARD_TYPE_REV);
        c.setType(QUEUE_TYPE_REV);
        c.flush();

        // fail the card
        col.reset();
        c = col.getSched().getCard();
        col.getSched().answerCard(c, 1);
        assertEquals(QUEUE_TYPE_LRN, c.getQueue());
        assertEquals(CARD_TYPE_RELEARNING, c.getType());
        assertEquals(1, c.getIvl());

        // immediately graduate it
        col.getSched().answerCard(c, 4);
        assertEquals(CARD_TYPE_REV, c.getType());
        assertEquals(QUEUE_TYPE_REV, c.getQueue());
        assertEquals(2, c.getIvl());
        assertEquals(col.getSched().getToday() + c.getIvl(), c.getDue());
    }


    @Test
    public void test_relearn_no_steps() throws Exception {
        Collection col = getColV2();
        Note note = col.newNote();
        note.setItem("Front", "one");
        col.addNote(note);
        Card c = note.cards().get(0);
        c.setIvl(100);
        c.setDue(col.getSched().getToday());
        c.setQueue(CARD_TYPE_REV);
        c.setType(QUEUE_TYPE_REV);
        c.flush();

        DeckConfig conf = col.getDecks().confForDid(1);
        conf.getJSONObject("lapse").put("delays", new JSONArray(new double[] {}));
        col.getDecks().save(conf);

        // fail the card
        col.reset();
        c = col.getSched().getCard();
        col.getSched().answerCard(c, 1);
        assertEquals(CARD_TYPE_REV, c.getType());
        assertEquals(QUEUE_TYPE_REV, c.getQueue());
    }


    @Test
    public void test_learn_collapsedV2() throws Exception {
        Collection col = getColV2();
        // add 2 notes
        Note note = col.newNote();
        note.setItem("Front", "1");
        col.addNote(note);
        note = col.newNote();
        note.setItem("Front", "2");
        col.addNote(note);
        // set as a learn card and rebuild queues
        col.getDb().execute("update cards set queue=0, type=0");
        col.reset();
        // should get '1' first
        Card c = col.getSched().getCard();
        assertTrue(c.q().endsWith("1"));
        // pass it so it's due in 10 minutes
        col.getSched().answerCard(c, 3);
        // get the other card
        c = col.getSched().getCard();
        assertTrue(c.q().endsWith("2"));
        // fail it so it's due in 1 minute
        col.getSched().answerCard(c, 1);
        // we shouldn't get the same card again
        c = col.getSched().getCard();
        assertFalse(c.q().endsWith("2"));
    }


    @Test
    public void test_learn_dayV2() throws Exception {
        Collection col = getColV2();
        // add a note
        Note note = col.newNote();
        note.setItem("Front", "one");
        col.addNote(note);
        col.getSched().reset();
        Card c = col.getSched().getCard();
        DeckConfig conf = col.getSched()._cardConf(c);
        conf.getJSONObject("new").put("delays", new JSONArray(new double[] {1, 10, 1440, 2880}));
        col.getDecks().save(conf);
        // pass it
        col.getSched().answerCard(c, 3);
        // two reps to graduate, 1 more today
        assertEquals(3, c.getLeft() % 1000);
        assertEquals(1, c.getLeft() / 1000);
        assertArrayEquals(new int[] {0, 1, 0}, col.getSched().counts());
        c = col.getSched().getCard();

        assertEquals(86400, col.getSched().nextIvl(c, 3));
        // answering it will place it in queue 3
        col.getSched().answerCard(c, 3);
        assertEquals(col.getSched().getToday() + 1, c.getDue());
        assertEquals(QUEUE_TYPE_DAY_LEARN_RELEARN, c.getQueue());
        assertNull(col.getSched().getCard());
        // for testing, move it back a day
        c.setDue(c.getDue() - 1);
        c.flush();
        col.reset();
        assertArrayEquals(new int[] {0, 1, 0}, col.getSched().counts());
        c = col.getSched().getCard();
        // nextIvl should work
        assertEquals(86400 * 2, col.getSched().nextIvl(c, 3));
        // if we fail it, it should be back in the correct queue
        col.getSched().answerCard(c, 1);
        assertEquals(QUEUE_TYPE_LRN, c.getQueue());
        col.undo();
        col.reset();
        c = col.getSched().getCard();
        col.getSched().answerCard(c, 3);
        // simulate the passing of another two days
        c.setDue(c.getDue() - 2);
        c.flush();
        col.reset();
        // the last pass should graduate it into a review card
        assertEquals(86400, col.getSched().nextIvl(c, 3));
        col.getSched().answerCard(c, 3);
        assertEquals(CARD_TYPE_REV, c.getType());
        assertEquals(QUEUE_TYPE_REV, c.getQueue());
        // if the lapse step is tomorrow, failing it should handle the counts
        // correctly
        c.setDue(0);
        c.flush();
        col.reset();
        assertArrayEquals(new int[] {0, 0, 1}, col.getSched().counts());
        conf = col.getSched()._cardConf(c);
        conf.getJSONObject("lapse").put("delays", new JSONArray(new double[] {1440}));
        col.getDecks().save(conf);
        c = col.getSched().getCard();
        col.getSched().answerCard(c, 1);
        assertEquals(QUEUE_TYPE_DAY_LEARN_RELEARN, c.getQueue());
        assertArrayEquals(new int[] {0, 0, 0}, col.getSched().counts());
    }


    @Test
    public void test_reviewsV2() throws Exception {
        Collection col = getColV2();
        // add a note
        Note note = col.newNote();
        note.setItem("Front", "one");
        note.setItem("Back", "two");
        col.addNote(note);
        // set the card up as a review card, due 8 days ago
        Card c = note.cards().get(0);
        c.setType(CARD_TYPE_REV);
        c.setQueue(QUEUE_TYPE_REV);
        c.setDue(col.getSched().getToday() - 8);
        c.setFactor(STARTING_FACTOR);
        c.setReps(3);
        c.setLapses(1);
        c.setIvl(100);
        c.startTimer();
        c.flush();
        // save it for later use as well
        Card cardcopy = c.clone();
        // try with an ease of 2
        ////////////////////////////////////////////////////////////////////////////////////////////////////
        c = cardcopy.clone();
        c.flush();
        col.reset();
        col.getSched().answerCard(c, 2);
        assertEquals(QUEUE_TYPE_REV, c.getQueue());
        // the new interval should be (100) * 1.2 = 120
        assertTrue(checkRevIvl(col, c, 120));
        assertEquals(col.getSched().getToday() + c.getIvl(), c.getDue());
        // factor should have been decremented
        assertEquals(2350, c.getFactor());
        // check counters
        assertEquals(1, c.getLapses());
        assertEquals(4, c.getReps());
        // ease 3
        ////////////////////////////////////////////////////////////////////////////////////////////////////
        c = cardcopy.clone();
        c.flush();
        col.getSched().answerCard(c, 3);
        // the new interval should be (100 + 8/2) * 2.5 = 260
        assertTrue(checkRevIvl(col, c, 260));
        assertEquals(col.getSched().getToday() + c.getIvl(), c.getDue());
        // factor should have been left alone
        assertEquals(STARTING_FACTOR, c.getFactor());
        // ease 4
        ////////////////////////////////////////////////////////////////////////////////////////////////////
        c = cardcopy.clone();
        c.flush();
        col.getSched().answerCard(c, 4);
        // the new interval should be (100 + 8) * 2.5 * 1.3 = 351
        assertTrue(checkRevIvl(col, c, 351));
        assertEquals(col.getSched().getToday() + c.getIvl(), c.getDue());
        // factor should have been increased
        assertEquals(2650, c.getFactor());
        // leech handling
        ////////////////////////////////////////////////////////////////////////////////////////////////////
        DeckConfig conf = col.getDecks().getConf(1);
        conf.getJSONObject("lapse").put("leechAction", LEECH_SUSPEND);
        col.getDecks().save(conf);
        c = cardcopy.clone();
        c.setLapses(7);
        c.flush();
        /* todo hook
        // steup hook
        hooked = new [] {};

        def onLeech(card):
        hooked.append(1);

        hooks.card_did_leech.append(onLeech);
        col.getSched().answerCard(c, 1);
        assertTrue(hooked);
        assertEquals(QUEUE_TYPE_SUSPENDED, c.getQueue());
        c.load();
        assertEquals(QUEUE_TYPE_SUSPENDED, c.getQueue());
        */
    }


    @Test
    public void test_review_limits() throws Exception {
        Collection col = getColV2();

        Deck parent = col.getDecks().get(col.getDecks().id("parent"));
        Deck child = col.getDecks().get(col.getDecks().id("parent::child"));

        DeckConfig pconf = col.getDecks().getConf(col.getDecks().confId("parentConf"));
        DeckConfig cconf = col.getDecks().getConf(col.getDecks().confId("childConf"));

        pconf.getJSONObject("rev").put("perDay", 5);
        col.getDecks().updateConf(pconf);
        col.getDecks().setConf(parent, pconf.getLong("id"));
        cconf.getJSONObject("rev").put("perDay", 10);
        col.getDecks().updateConf(cconf);
        col.getDecks().setConf(child, cconf.getLong("id"));

        Model m = col.getModels().current();
        m.put("did", child.getLong("id"));
        col.getModels().save(m, false);

        // add some cards
        for (int i = 0; i < 20; i++) {
            Note note = col.newNote();
            note.setItem("Front", "one");
            note.setItem("Back", "two");
            col.addNote(note);

            // make them reviews
            Card c = note.cards().get(0);
            c.setQueue(CARD_TYPE_REV);
            c.setType(QUEUE_TYPE_REV);
            c.setDue(0);
            c.flush();
        }

        // position 0 is default deck. Different from upstream
        DeckDueTreeNode tree = col.getSched().deckDueTree().get(1);
        // (('parent', 1514457677462, 5, 0, 0, (('child', 1514457677463, 5, 0, 0, ()),)))
        assertEquals("parent", tree.getFullDeckName());
        assertEquals(5, tree.getRevCount());  // paren, tree.review_count)t
        assertEquals(5, tree.getChildren().get(0).getRevCount());

        // .counts() should match
        col.getDecks().select(child.getLong("id"));
        col.getSched().reset();
        assertArrayEquals(new int[] {0, 0, 5}, col.getSched().counts());

        // answering a card in the child should decrement parent count
        Card c = col.getSched().getCard();
        col.getSched().answerCard(c, 3);
        assertArrayEquals(new int[] {0, 0, 4}, col.getSched().counts());

        tree = col.getSched().deckDueTree().get(1);
        assertEquals(4, tree.getRevCount());
        assertEquals(4, tree.getChildren().get(0).getRevCount());
    }


    @Test
    public void test_button_spacingV2() throws Exception {
        Collection col = getColV2();
        Note note = col.newNote();
        note.setItem("Front", "one");
        col.addNote(note);
        // 1 day ivl review card due now
        Card c = note.cards().get(0);
        c.setType(CARD_TYPE_REV);
        c.setQueue(QUEUE_TYPE_REV);
        c.setDue(col.getSched().getToday());
        c.setReps(1);
        c.setIvl(1);
        c.startTimer();
        c.flush();
        col.reset();
        // Upstream, there is no space in 2d
        assertEquals("2 d", without_unicode_isolation(col.getSched().nextIvlStr(getTargetContext(), c, 2)));
        assertEquals("3 d", without_unicode_isolation(col.getSched().nextIvlStr(getTargetContext(), c, 3)));
        assertEquals("4 d", without_unicode_isolation(col.getSched().nextIvlStr(getTargetContext(), c, 4)));

        // if hard factor is <= 1, then hard may not increase
        DeckConfig conf = col.getDecks().confForDid(1);
        conf.getJSONObject("rev").put("hardFactor", 1);
        col.getDecks().save(conf);
        assertEquals("1 d", without_unicode_isolation(col.getSched().nextIvlStr(getTargetContext(), c, 2)));
    }


    @Test
    @Ignore("Disabled upstream")
    public void test_overdue_lapseV2() throws Exception {
        // disabled in commit 3069729776990980f34c25be66410e947e9d51a2
        return;
        /* Upstream does not execute it
           Collection col = getColV2()  // pylint: disable=unreachable
           // add a note
           Note note = col.newNote();
           note.setItem("Front","one");
           col.addNote(note);
           // simulate a review that was lapsed and is now due for its normal review
           Card c = note.cards().get(0);
           c.setType(CARD_TYPE_REV);
           c.setQueue(QUEUE_TYPE_LRN);
           c.setDue(-1);
           c.setODue(-1);
           c.setFactor(STARTING_FACTOR);
           c.setLeft(2002);
           c.setIvl(0);
           c.flush();
           // checkpoint
           col.save();
           col.getSched().reset();
           assertArrayEquals(new int[]{0, 2, 0}, col.getSched().counts());
           c = col.getSched().getCard();
           col.getSched().answerCard(c, 3);
           // it should be due tomorrow
           assertEquals(col.getSched().getToday()+ 1, c.getDue());
           // revert to before
           /* todo: rollback
           col.rollback();
           // with the default settings, the overdue card should be removed from the
           // learning queue
           col.getSched().reset();
           assertArrayEquals(new int[]{0, 0, 1}, col.getSched().counts());
        */

    }


    @Test
    public void test_finishedV2() throws Exception {
        Collection col = getColV2();
        // nothing due
        assertThat(col.getSched().finishedMsg(getTargetContext()).toString(), containsString("Congratulations"));
        assertThat(col.getSched().finishedMsg(getTargetContext()).toString(), not(containsString("limit")));
        Note note = col.newNote();
        note.setItem("Front", "one");
        note.setItem("Back", "two");
        col.addNote(note);
        // have a new card
        assertThat(col.getSched().finishedMsg(getTargetContext()).toString(), containsString("new cards available"));
        // turn it into a review
        col.reset();
        Card c = note.cards().get(0);
        c.startTimer();
        col.getSched().answerCard(c, 3);
        // nothing should be due tomorrow, as it's due in a week
        assertThat(col.getSched().finishedMsg(getTargetContext()).toString(), containsString("Congratulations"));
        assertThat(col.getSched().finishedMsg(getTargetContext()).toString(), not(containsString("limit")));
    }


    @Test
    public void test_nextIvlV2() throws Exception {
        Collection col = getColV2();
        Note note = col.newNote();
        note.setItem("Front", "one");
        note.setItem("Back", "two");
        col.addNote(note);
        col.reset();
        DeckConfig conf = col.getDecks().confForDid(1);
        conf.getJSONObject("new").put("delays", new JSONArray(new double[] {0.5, 3, 10}));
        conf.getJSONObject("lapse").put("delays", new JSONArray(new double[] {1, 5, 9}));
        col.getDecks().save(conf);
        Card c = col.getSched().getCard();
        // new cards
        ////////////////////////////////////////////////////////////////////////////////////////////////////

        assertEquals(30, col.getSched().nextIvl(c, 1));
        assertEquals((30 + 180) / 2, col.getSched().nextIvl(c, 2));
        assertEquals(180, col.getSched().nextIvl(c, 3));
        assertEquals(4 * 86400, col.getSched().nextIvl(c, 4));
        col.getSched().answerCard(c, 1);
        // cards in learning
        ////////////////////////////////////////////////////////////////////////////////////////////////////
        assertEquals(30, col.getSched().nextIvl(c, 1));
        assertEquals((30 + 180) / 2, col.getSched().nextIvl(c, 2));
        assertEquals(180, col.getSched().nextIvl(c, 3));
        assertEquals(4 * 86400, col.getSched().nextIvl(c, 4));
        col.getSched().answerCard(c, 3);
        assertEquals(30, col.getSched().nextIvl(c, 1));
        assertEquals((180 + 600) / 2, col.getSched().nextIvl(c, 2));
        assertEquals(600, col.getSched().nextIvl(c, 3));
        assertEquals(4 * 86400, col.getSched().nextIvl(c, 4));
        col.getSched().answerCard(c, 3);
        // normal graduation is tomorrow
        assertEquals(1 * 86400, col.getSched().nextIvl(c, 3));
        assertEquals(4 * 86400, col.getSched().nextIvl(c, 4));
        // lapsed cards
        ////////////////////////////////////////////////////////////////////////////////////////////////////
        c.setType(CARD_TYPE_REV);
        c.setIvl(100);
        c.setFactor(STARTING_FACTOR);
        assertEquals(60, col.getSched().nextIvl(c, 1));
        assertEquals(100 * 86400, col.getSched().nextIvl(c, 3));
        assertEquals(101 * 86400, col.getSched().nextIvl(c, 4));
        // review cards
        ////////////////////////////////////////////////////////////////////////////////////////////////////
        c.setQueue(QUEUE_TYPE_REV);
        c.setIvl(100);
        c.setFactor(STARTING_FACTOR);
        // failing it should put it at 60s
        assertEquals(60, col.getSched().nextIvl(c, 1));
        // or 1 day if relearn is false
        conf.getJSONObject("lapse").put("delays", new JSONArray(new double[] {}));
        col.getDecks().save(conf);
        assertEquals(1 * 86400, col.getSched().nextIvl(c, 1));
        // (* 100 1.2 86400)10368000.0
        assertEquals(10368000, col.getSched().nextIvl(c, 2));
        // (* 100 2.5 86400)21600000.0
        assertEquals(21600000, col.getSched().nextIvl(c, 3));
        // (* 100 2.5 1.3 86400)28080000.0
        assertEquals(28080000, col.getSched().nextIvl(c, 4));

        assertThat(without_unicode_isolation(col.getSched().nextIvlStr(getTargetContext(), c, 4)), is("10.8 mo"));
    }


    @Test
    public void test_bury() throws Exception {
        Collection col = getColV2();
        Note note = col.newNote();
        note.setItem("Front", "one");
        col.addNote(note);
        Card c = note.cards().get(0);
        note = col.newNote();
        note.setItem("Front", "two");
        col.addNote(note);
        Card c2 = note.cards().get(0);
        // burying
        col.getSched().buryCards(new long[] {c.getId()}, true);
        c.load();
        assertEquals(QUEUE_TYPE_MANUALLY_BURIED, c.getQueue());
        col.getSched().buryCards(new long[] {c2.getId()}, false);
        c2.load();
        assertEquals(QUEUE_TYPE_SIBLING_BURIED, c2.getQueue());

        col.reset();
        assertNull(col.getSched().getCard());

        col.getSched().unburyCardsForDeck(AbstractSched.UnburyType.MANUAL);
        c.load();
        assertEquals(QUEUE_TYPE_NEW, c.getQueue());
        c2.load();
        assertEquals(QUEUE_TYPE_SIBLING_BURIED, c2.getQueue());

        col.getSched().unburyCardsForDeck(AbstractSched.UnburyType.SIBLINGS);
        c2.load();
        assertEquals(QUEUE_TYPE_NEW, c2.getQueue());

        col.getSched().buryCards(new long[] {c.getId(), c2.getId()});
        col.getSched().unburyCardsForDeck(AbstractSched.UnburyType.ALL);

        col.reset();

        assertArrayEquals(new int[] {2, 0, 0}, col.getSched().counts());
    }


    @Test
    public void test_suspendv2() throws Exception {
        Collection col = getColV2();
        Note note = col.newNote();
        note.setItem("Front", "one");
        col.addNote(note);
        Card c = note.cards().get(0);
        // suspending
        col.reset();
        assertNotNull(col.getSched().getCard());
        col.getSched().suspendCards(new long[] {c.getId()});
        col.reset();
        assertNull(col.getSched().getCard());
        // unsuspending
        col.getSched().unsuspendCards(new long[] {c.getId()});
        col.reset();
        assertNotNull(col.getSched().getCard());
        // should cope with rev cards being relearnt
        c.setDue(0);
        c.setIvl(100);
        c.setType(CARD_TYPE_REV);
        c.setQueue(QUEUE_TYPE_REV);
        c.flush();
        col.reset();
        c = col.getSched().getCard();
        col.getSched().answerCard(c, 1);
        assertTrue(c.getDue() >= now());
        long due = c.getDue();
        assertEquals(QUEUE_TYPE_LRN, c.getQueue());
        assertEquals(CARD_TYPE_RELEARNING, c.getType());
        col.getSched().suspendCards(new long[] {c.getId()});
        col.getSched().unsuspendCards(new long[] {c.getId()});
        c.load();
        assertEquals(QUEUE_TYPE_LRN, c.getQueue());
        assertEquals(CARD_TYPE_RELEARNING, c.getType());
        assertEquals(due, c.getDue());
        // should cope with cards in cram decks
        c.setDue(1);
        c.flush();
        col.getDecks().newDyn("tmp");
        col.getSched().rebuildDyn();
        c.load();
        assertNotEquals(1, c.getDue());
        assertNotEquals(1, c.getDid());
        col.getSched().suspendCards(new long[] {c.getId()});
        c.load();
        assertNotEquals(1, c.getDue());
        assertNotEquals(1, c.getDid());
        assertEquals(1, c.getODue());
    }


    @Test
    public void test_filt_reviewing_early_normal() throws Exception {
        Collection col = getColV2();
        Note note = col.newNote();
        note.setItem("Front", "one");
        col.addNote(note);
        Card c = note.cards().get(0);
        c.setIvl(100);
        c.setQueue(CARD_TYPE_REV);
        c.setType(QUEUE_TYPE_REV);
        // due in 25 days, so it's been waiting 75 days
        c.setDue(col.getSched().getToday() + 25);
        c.setMod(1);
        c.setFactor(STARTING_FACTOR);
        c.startTimer();
        c.flush();
        col.reset();
        assertArrayEquals(new int[] {0, 0, 0}, col.getSched().counts());
        // create a dynamic deck and refresh it
        long did = col.getDecks().newDyn("Cram");
        col.getSched().rebuildDyn(did);
        col.reset();
        // should appear as normal in the deck list
        /* todo sort
           assertEquals(1, sorted(col.getSched().deckDueTree().getChildren())[0].review_count);
        */
        // and should appear in the counts
        assertArrayEquals(new int[] {0, 0, 1}, col.getSched().counts());
        // grab it and check estimates
        c = col.getSched().getCard();
        assertEquals(4, col.getSched().answerButtons(c));
        assertEquals(600, col.getSched().nextIvl(c, 1));
        assertEquals(Math.round(75 * 1.2) * 86400, col.getSched().nextIvl(c, 2));
        assertThat(col.getSched().nextIvl(c, 3), is((long)(75 * 2.5) * 86400));
        assertThat(col.getSched().nextIvl(c, 4), is((long)(75 * 2.5 * 1.15) * 86400));

        // answer 'good'
        col.getSched().answerCard(c, 3);
        checkRevIvl(col, c, 90);
        assertEquals(col.getSched().getToday() + c.getIvl(), c.getDue());
        assertEquals(0L, c.getODue());
        // should not be in learning
        assertEquals(QUEUE_TYPE_REV, c.getQueue());
        // should be logged as a cram rep
        assertEquals(3, col.getDb().queryLongScalar("select type from revlog order by id desc limit 1"));

        // due in 75 days, so it's been waiting 25 days
        c.setIvl(100);
        c.setDue(col.getSched().getToday() + 75);
        c.flush();
        col.getSched().rebuildDyn(did);
        col.reset();
        c = col.getSched().getCard();

        assertEquals(60 * 86400, col.getSched().nextIvl(c, 2));
        assertEquals(100 * 86400, col.getSched().nextIvl(c, 3));
        assertEquals(114 * 86400, col.getSched().nextIvl(c, 4));
    }


    @Test
    public void test_filt_keep_lrn_state() throws Exception {
        Collection col = getColV2();

        Note note = col.newNote();
        note.setItem("Front", "one");
        col.addNote(note);

        // fail the card outside filtered deck
        Card c = col.getSched().getCard();
        DeckConfig conf = col.getSched()._cardConf(c);
        conf.getJSONObject("new").put("delays", new JSONArray(new double[] {1, 10, 61}));
        col.getDecks().save(conf);

        col.getSched().answerCard(c, 1);

        assertEquals(CARD_TYPE_LRN, c.getQueue());
        assertEquals(QUEUE_TYPE_LRN, c.getType());
        assertEquals(3003, c.getLeft());

        col.getSched().answerCard(c, 3);
        assertEquals(CARD_TYPE_LRN, c.getQueue());
        assertEquals(QUEUE_TYPE_LRN, c.getType());

        // create a dynamic deck and refresh it
        long did = col.getDecks().newDyn("Cram");
        col.getSched().rebuildDyn(did);
        col.reset();

        // card should still be in learning state
        c.load();
        assertEquals(CARD_TYPE_LRN, c.getQueue());
        assertEquals(QUEUE_TYPE_LRN, c.getType());
        assertEquals(2002, c.getLeft());

        // should be able to advance learning steps
        col.getSched().answerCard(c, 3);
        // should be due at least an hour in the future
        assertTrue(c.getDue() - intTime() > 60 * 60);

        // emptying the deck preserves learning state
        col.getSched().emptyDyn(did);
        c.load();
        assertEquals(CARD_TYPE_LRN, c.getQueue());
        assertEquals(QUEUE_TYPE_LRN, c.getType());
        assertEquals(1001, c.getLeft());
        assertTrue(c.getDue() - intTime() > 60 * 60);
    }


    @Test
    public void test_preview() throws Exception {
        // add cards
        Collection col = getColV2();
        Note note = col.newNote();
        note.setItem("Front", "one");
        col.addNote(note);
        Card c = note.cards().get(0);
        Card orig = c.clone();
        Note note2 = col.newNote();
        note2.setItem("Front", "two");
        col.addNote(note2);
        // cram deck
        long did = col.getDecks().newDyn("Cram");
        Deck cram = col.getDecks().get(did);
        cram.put("resched", false);
        col.getDecks().save(cram);
        col.getSched().rebuildDyn(did);
        col.reset();
        // grab the first card
        c = col.getSched().getCard();
        assertEquals(2, col.getSched().answerButtons(c));
        assertEquals(600, col.getSched().nextIvl(c, 1));
        assertEquals(0, col.getSched().nextIvl(c, 2));
        // failing it will push its due time back
        long due = c.getDue();
        col.getSched().answerCard(c, 1);
        assertNotEquals(c.getDue(), due);

        // the other card should come next
        Card c2 = col.getSched().getCard();
        assertNotEquals(c2.getId(), c.getId());

        // passing it will remove it
        col.getSched().answerCard(c2, 2);
        assertEquals(QUEUE_TYPE_NEW, c2.getQueue());
        assertEquals(0, c2.getReps());
        assertEquals(CARD_TYPE_NEW, c2.getType());

        // the other card should appear again
        c = col.getSched().getCard();
        assertEquals(orig.getId(), c.getId());

        // emptying the filtered deck should restore card
        col.getSched().emptyDyn(did);
        c.load();
        assertEquals(QUEUE_TYPE_NEW, c.getQueue());
        assertEquals(0, c.getReps());
        assertEquals(CARD_TYPE_NEW, c.getType());
    }


    @Test
    public void test_ordcycleV2() throws Exception {
        Collection col = getColV2();
        // add two more templates and set second active
        Model m = col.getModels().current();
        Models mm = col.getModels();
        JSONObject t = mm.newTemplate("Reverse");
        t.put("qfmt", "{{Back}}");
        t.put("afmt", "{{Front}}");
        mm.addTemplateModChanged(m, t);
        t = mm.newTemplate("f2");
        t.put("qfmt", "{{Front}}");
        t.put("afmt", "{{Back}}");
        mm.addTemplateModChanged(m, t);
        mm.save(m);
        // create a new note; it should have 3 cards
        Note note = col.newNote();
        note.setItem("Front", "1");
        note.setItem("Back", "1");
        col.addNote(note);
        assertEquals(3, col.cardCount());
        col.reset();
        // ordinals should arrive in order
        AbstractSched sched = col.getSched();
        Card c = sched.getCard();
        sched.answerCard(c, sched.answerButtons(c) - 1); // not upstream. But we are not expecting multiple getCard without review
        assertEquals(0, c.getOrd());
        c = sched.getCard();
        sched.answerCard(c, sched.answerButtons(c) - 1); // not upstream. But we are not expecting multiple getCard without review
        assertEquals(1, c.getOrd());
        c = sched.getCard();
        sched.answerCard(c, sched.answerButtons(c) - 1); // not upstream. But we are not expecting multiple getCard without review
        assertEquals(2, c.getOrd());
    }


    @Test
    public void test_counts_idxV2() throws Exception {
        Collection col = getColV2();
        Note note = col.newNote();
        note.setItem("Front", "one");
        note.setItem("Back", "two");
        col.addNote(note);
        col.reset();
        assertArrayEquals(new int[] {1, 0, 0}, col.getSched().counts());
        Card c = col.getSched().getCard();
        // counter's been decremented but idx indicates 1
        assertArrayEquals(new int[] {0, 0, 0}, col.getSched().counts());
        assertEquals(0, col.getSched().countIdx(c));
        // answer to move to learn queue
        col.getSched().answerCard(c, 1);
        assertArrayEquals(new int[] {0, 1, 0}, col.getSched().counts());
        // fetching again will decrement the count
        c = col.getSched().getCard();
        assertArrayEquals(new int[] {0, 0, 0}, col.getSched().counts());
        assertEquals(1, col.getSched().countIdx(c));
        // answering should add it back again
        col.getSched().answerCard(c, 1);
        assertArrayEquals(new int[] {0, 1, 0}, col.getSched().counts());
    }


    @Test
    public void test_repCountsV2() throws Exception {
        Collection col = getColV2();
        Note note = col.newNote();
        note.setItem("Front", "one");
        col.addNote(note);
        col.reset();
        // lrnReps should be accurate on pass/fail
        assertArrayEquals(new int[] {1, 0, 0}, col.getSched().counts());
        col.getSched().answerCard(col.getSched().getCard(), 1);
        assertArrayEquals(new int[] {0, 1, 0}, col.getSched().counts());
        col.getSched().answerCard(col.getSched().getCard(), 1);
        assertArrayEquals(new int[] {0, 1, 0}, col.getSched().counts());
        col.getSched().answerCard(col.getSched().getCard(), 3);
        assertArrayEquals(new int[] {0, 1, 0}, col.getSched().counts());
        col.getSched().answerCard(col.getSched().getCard(), 1);
        assertArrayEquals(new int[] {0, 1, 0}, col.getSched().counts());
        col.getSched().answerCard(col.getSched().getCard(), 3);
        assertArrayEquals(new int[] {0, 1, 0}, col.getSched().counts());
        col.getSched().answerCard(col.getSched().getCard(), 3);
        assertArrayEquals(new int[] {0, 0, 0}, col.getSched().counts());
        note = col.newNote();
        note.setItem("Front", "two");
        col.addNote(note);
        col.reset();
        // initial pass should be correct too
        col.getSched().answerCard(col.getSched().getCard(), 3);
        assertArrayEquals(new int[] {0, 1, 0}, col.getSched().counts());
        col.getSched().answerCard(col.getSched().getCard(), 1);
        assertArrayEquals(new int[] {0, 1, 0}, col.getSched().counts());
        col.getSched().answerCard(col.getSched().getCard(), 4);
        assertArrayEquals(new int[] {0, 0, 0}, col.getSched().counts());
        // immediate graduate should work
        note = col.newNote();
        note.setItem("Front", "three");
        col.addNote(note);
        col.reset();
        col.getSched().answerCard(col.getSched().getCard(), 4);
        assertArrayEquals(new int[] {0, 0, 0}, col.getSched().counts());
        // and failing a review should too
        note = col.newNote();
        note.setItem("Front", "three");
        col.addNote(note);
        Card c = note.cards().get(0);
        c.setType(CARD_TYPE_REV);
        c.setQueue(QUEUE_TYPE_REV);
        c.setDue(col.getSched().getToday());
        c.flush();
        col.reset();
        assertArrayEquals(new int[] {0, 0, 1}, col.getSched().counts());
        col.getSched().answerCard(col.getSched().getCard(), 1);
        assertArrayEquals(new int[] {0, 1, 0}, col.getSched().counts());
    }


    @Test
    public void test_timingV2() throws Exception {
        Collection col = getColV2();
        // add a few review cards, due today
        for (int i = 0; i < 5; i++) {
            Note note = col.newNote();
            note.setItem("Front", "num" + i);
            col.addNote(note);
            Card c = note.cards().get(0);
            c.setType(CARD_TYPE_REV);
            c.setQueue(QUEUE_TYPE_REV);
            c.setDue(0);
            c.flush();
        }
        // fail the first one
        col.reset();
        Card c = col.getSched().getCard();
        col.getSched().answerCard(c, 1);
        // the next card should be another review
        Card c2 = col.getSched().getCard();
        assertEquals(QUEUE_TYPE_REV, c2.getQueue());
        // if the failed card becomes due, it should show first
        c.setDue(intTime() - 1);
        c.flush();
        col.reset();
        c = col.getSched().getCard();
        assertEquals(QUEUE_TYPE_LRN, c.getQueue());
    }


    @Test
    public void test_collapseV2() throws Exception {
        Collection col = getColV2();
        // add a note
        Note note = col.newNote();
        note.setItem("Front", "one");
        col.addNote(note);
        col.reset();
        // test collapsing
        Card c = col.getSched().getCard();
        col.getSched().answerCard(c, 1);
        c = col.getSched().getCard();
        col.getSched().answerCard(c, 4);
        assertNull(col.getSched().getCard());
    }


    @Test
    public void test_deckDueV2() throws Exception {
        Collection col = getColV2();
        // add a note with default deck
        Note note = col.newNote();
        note.setItem("Front", "one");
        col.addNote(note);
        // and one that's a child
        note = col.newNote();
        note.setItem("Front", "two");
        long default1 = col.getDecks().id("Default::1");
        note.model().put("did", default1);
        col.addNote(note);
        // make it a review card
        Card c = note.cards().get(0);
        c.setQueue(QUEUE_TYPE_REV);
        c.setDue(0);
        c.flush();
        // add one more with a new deck
        note = col.newNote();
        note.setItem("Front", "two");
        note.model().put("did", col.getDecks().id("foo::bar"));
        col.addNote(note);
        // and one that's a sibling
        note = col.newNote();
        note.setItem("Front", "three");
        note.model().put("did", col.getDecks().id("foo::baz"));
        col.addNote(note);
        col.reset();
        assertEquals(5, col.getDecks().allSortedNames().size());
        DeckDueTreeNode tree = col.getSched().deckDueTree().get(0);
        assertEquals("Default", tree.getLastDeckNameComponent());
        // sum of child and parent
        assertEquals(1, tree.getDid());
        assertEquals(1, tree.getRevCount());
        assertEquals(1, tree.getNewCount());
        // child count is just review
        DeckDueTreeNode child = tree.getChildren().get(0);
        assertEquals("1", child.getLastDeckNameComponent());
        assertEquals(default1, child.getDid());
        assertEquals(1, child.getRevCount());
        assertEquals(0, child.getNewCount());
        // code should not fail if a card has an invalid deck
        c.setDid(12345);
        c.flush();
        col.getSched().deckDueTree();
    }


    @Test
    public void test_deckTree() throws Exception {
        Collection col = getColV2();
        col.getDecks().id("new::b::c");
        col.getDecks().id("new2");
        // new should not appear twice in tree
        List<String> names = new ArrayList<>();
        for (DeckDueTreeNode tree : col.getSched().deckDueTree()) {
            names.add(tree.getLastDeckNameComponent());
        }
        names.remove("new");
        assertFalse(names.contains("new"));
    }


    @Test
    public void test_deckFlowV2() throws Exception {
        Collection col = getColV2();
        // add a note with default deck
        Note note = col.newNote();
        note.setItem("Front", "one");
        col.addNote(note);
        // and one that's a child
        note = col.newNote();
        note.setItem("Front", "two");
        long default1 = col.getDecks().id("Default::2");
        note.model().put("did", default1);
        col.addNote(note);
        // and another that's higher up
        note = col.newNote();
        note.setItem("Front", "three");
        default1 = col.getDecks().id("Default::1");
        note.model().put("did", default1);
        col.addNote(note);
        // should get top level one first, then ::1, then ::2
        col.reset();
        assertArrayEquals(new int[] {3, 0, 0}, col.getSched().counts());
        for (String i : new String[] {"one", "three", "two"}) {
            Card c = col.getSched().getCard();
            assertEquals(i, c.note().getItem("Front"));
            col.getSched().answerCard(c, 3);
        }
    }


    @Test
    public void test_reorder() throws Exception {
        Collection col = getColV2();
        // add a note with default deck
        Note note = col.newNote();
        note.setItem("Front", "one");
        col.addNote(note);
        Note note2 = col.newNote();
        note2.setItem("Front", "two");
        col.addNote(note2);
        assertEquals(2, note2.cards().get(0).getDue());
        boolean found = false;
        // 50/50 chance of being reordered
        for (int i = 0; i < 20; i++) {
            col.getSched().randomizeCards(1);
            if (note.cards().get(0).getDue() != note.getId()) {
                found = true;
                break;
            }
        }
        assertTrue(found);
        col.getSched().orderCards(1);
        assertEquals(1, note.cards().get(0).getDue());
        // shifting
        Note note3 = col.newNote();
        note3.setItem("Front", "three");
        col.addNote(note3);
        Note note4 = col.newNote();
        note4.setItem("Front", "four");
        col.addNote(note4);
        assertEquals(1, note.cards().get(0).getDue());
        assertEquals(2, note2.cards().get(0).getDue());
        assertEquals(3, note3.cards().get(0).getDue());
        assertEquals(4, note4.cards().get(0).getDue());
        /* todo: start
           col.getSched().sortCards(new long [] {note3.cards().get(0).getId(), note4.cards().get(0).getId()}, start=1, shift=true);
           assertEquals(3, note.cards().get(0).getDue());
           assertEquals(4, note2.cards().get(0).getDue());
           assertEquals(1, note3.cards().get(0).getDue());
           assertEquals(2, note4.cards().get(0).getDue());
        */
    }


    @Test
    public void test_forgetV2() throws Exception {
        Collection col = getColV2();
        Note note = col.newNote();
        note.setItem("Front", "one");
        col.addNote(note);
        Card c = note.cards().get(0);
        c.setQueue(QUEUE_TYPE_REV);
        c.setType(CARD_TYPE_REV);
        c.setIvl(100);
        c.setDue(0);
        c.flush();
        col.reset();
        assertArrayEquals(new int[] {0, 0, 1}, col.getSched().counts());
        col.getSched().forgetCards(new long[] {c.getId()});
        col.reset();
        assertArrayEquals(new int[] {1, 0, 0}, col.getSched().counts());
    }


    @Test
    public void test_reschedV2() throws Exception {
        Collection col = getColV2();
        Note note = col.newNote();
        note.setItem("Front", "one");
        col.addNote(note);
        Card c = note.cards().get(0);
        col.getSched().reschedCards(new long[] {c.getId()}, 0, 0);
        c.load();
        assertEquals(col.getSched().getToday(), c.getDue());
        assertEquals(1, c.getIvl());
        assertEquals(QUEUE_TYPE_REV, c.getType());
        assertEquals(CARD_TYPE_REV, c.getQueue());
        col.getSched().reschedCards(new long[] {c.getId()}, 1, 1);
        c.load();
        assertEquals(col.getSched().getToday() + 1, c.getDue());
        assertEquals(+1, c.getIvl());
    }


    @Test
    public void test_norelearnV2() throws Exception {
        Collection col = getColV2();
        // add a note
        Note note = col.newNote();
        note.setItem("Front", "one");
        col.addNote(note);
        Card c = note.cards().get(0);
        c.setType(CARD_TYPE_REV);
        c.setQueue(QUEUE_TYPE_REV);
        c.setDue(0);
        c.setFactor(STARTING_FACTOR);
        c.setReps(3);
        c.setLapses(1);
        c.setIvl(100);
        c.startTimer();
        c.flush();
        col.reset();
        col.getSched().answerCard(c, 1);
        col.getSched()._cardConf(c).getJSONObject("lapse").put("delays", new JSONArray(new double[] {}));
        col.getSched().answerCard(c, 1);
    }


    @Test
    public void test_failmultV2() throws Exception {
        Collection col = getColV2();
        Note note = col.newNote();
        note.setItem("Front", "one");
        note.setItem("Back", "two");
        col.addNote(note);
        Card c = note.cards().get(0);
        c.setType(CARD_TYPE_REV);
        c.setQueue(QUEUE_TYPE_REV);
        c.setIvl(100);
        c.setDue(col.getSched().getToday() - c.getIvl());
        c.setFactor(STARTING_FACTOR);
        c.setReps(3);
        c.setLapses(1);
        c.startTimer();
        c.flush();
        DeckConfig conf = col.getSched()._cardConf(c);
        conf.getJSONObject("lapse").put("mult", 0.5);
        col.getDecks().save(conf);
        c = col.getSched().getCard();
        col.getSched().answerCard(c, 1);
        assertEquals(50, c.getIvl());
        col.getSched().answerCard(c, 1);
        assertEquals(25, c.getIvl());
    }


    @Test
    public void test_moveVersions() throws Exception {
        Collection col = getColV2();
        changeSchedulerVer(col, 1);

        Note n = col.newNote();
        n.setItem("Front", "one");
        col.addNote(n);

        // make it a learning card
        col.reset();
        Card c = col.getSched().getCard();
        col.getSched().answerCard(c, 1);

        // the move to v2 should reset it to new
        changeSchedulerVer(col, 2);
        c.load();
        assertEquals(QUEUE_TYPE_NEW, c.getQueue());
        assertEquals(CARD_TYPE_NEW, c.getType());

        // fail it again, and manually bury it
        col.reset();
        c = col.getSched().getCard();
        col.getSched().answerCard(c, 1);
        col.getSched().buryCards(new long[] {c.getId()});
        c.load();
        assertEquals(QUEUE_TYPE_MANUALLY_BURIED, c.getQueue());

        // revert to version 1
        changeSchedulerVer(col, 1);

        // card should have moved queues
        c.load();
        assertEquals(QUEUE_TYPE_SIBLING_BURIED, c.getQueue());

        // and it should be new again when unburied
        col.getSched().unburyCards();
        c.load();
        assertEquals(CARD_TYPE_NEW, c.getQueue());
        assertEquals(QUEUE_TYPE_NEW, c.getType());

        // make sure relearning cards transition correctly to v1
        changeSchedulerVer(col, 2);
        // card with 100 day interval, answering again
        col.getSched().reschedCards(new long[] {c.getId()}, 100, 100);
        c.load();
        c.setDue(0);
        c.flush();
        DeckConfig conf = col.getSched()._cardConf(c);
        conf.getJSONObject("lapse").put("mult", 0.5);
        col.getDecks().save(conf);
        col.getSched().reset();
        c = col.getSched().getCard();
        col.getSched().answerCard(c, 1);
        // due should be correctly set when removed from learning early
        changeSchedulerVer(col, 1);
        c.load();
        assertEquals(50, c.getDue());
    }


    // cards with a due date earlier than the collection should retain
    // their due date when removed
    @Test
    public void test_negativeDueFilter() throws Exception {
        Collection col = getColV2();

        // card due prior to collection date
        Note note = col.newNote();
        note.setItem("Front", "one");
        note.setItem("Back", "two");
        col.addNote(note);
        Card c = note.cards().get(0);
        c.setDue(-5);
        c.setQueue(QUEUE_TYPE_REV);
        c.setIvl(5);
        c.flush();

        // into and out of filtered deck
        long did = col.getDecks().newDyn("Cram");
        col.getSched().rebuildDyn(did);
        col.getSched().emptyDyn(did);
        col.reset();

        c.load();
        assertEquals(-5, c.getDue());
    }


    // hard on the first step should be the average of again and good,
    // and it should be logged properly


    @Test
    @Ignore("Port anki@a9c93d933cadbf5d9c7e3e2b4f7a25d2c59da5d3")
    public void test_initial_repeat() throws Exception {
        Collection col = getColV2();
        Note note = col.newNote();
        note.setItem("Front", "one");
        note.setItem("Back", "two");
        col.addNote(note);

        col.reset();
        Card c = col.getSched().getCard();
        col.getSched().answerCard(c, 2);
        // should be due in ~ 5.5 mins
        double expected = now() + 5.5 * 60;
        long due = c.getDue();
        assertTrue((expected - 10 < due) && (due < expected * 1.25));

        long ivl = col.getDb().queryLongScalar("select ivl from revlog");
        assertEquals((long) (-5.5 * 60), ivl);
    }
}
