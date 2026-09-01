package com.debelatabla.fifaleague;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.*;
import android.net.Uri;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.json.*;

public class MainActivity extends Activity {
  static final int BG = Color.rgb(8, 27, 22),
      CARD = Color.rgb(19, 50, 41),
      GREEN = Color.rgb(38, 208, 124),
      MUTED = Color.rgb(168, 194, 183);
  final ArrayList<String> players = new ArrayList<>();
  final ArrayList<String> teams = new ArrayList<>();
  final ArrayList<Game> games = new ArrayList<>();
  final ArrayList<String> queue = new ArrayList<>();
  final ArrayList<Integer> knockoutAdvancers = new ArrayList<>();
  final ArrayList<ArrayList<String>> knockoutBracketRounds = new ArrayList<>();
  LinearLayout root, namesBox;
  android.content.SharedPreferences prefs;
  String tournamentMode = "";
  String phase = "setup";
  int leagueMatchesPerPlayer = 0;
  int knockoutQualifiers = 0;
  int currentKnockoutRound = -1;
  long tournamentDateMillis = 0L;
  Button tournamentSettingsButton;

  static class Game {
    int a, b, ga, gb;
    boolean knockout;
    int knockoutRound;

    Game(int a, int b, int ga, int gb, boolean knockout, int knockoutRound) {
      this.a = a;
      this.b = b;
      this.ga = ga;
      this.gb = gb;
      this.knockout = knockout;
      this.knockoutRound = knockoutRound;
    }
  }

  @Override
  public void onCreate(Bundle b) {
    super.onCreate(b);
    prefs = getSharedPreferences("league", MODE_PRIVATE);
    load();
    if (players.size() < 2) setupScreen();
    else tableScreen();
  }

  TextView text(String s, int size, int color) {
    TextView v = new TextView(this);
    v.setText(s);
    v.setTextSize(size);
    v.setTextColor(color);
    v.setPadding(dp(12), dp(10), dp(12), dp(10));
    return v;
  }

  Button button(String s) {
    Button b = new Button(this);
    b.setText(s);
    b.setTextColor(BG);
    b.setTextSize(15);
    b.setTypeface(null, Typeface.BOLD);
    b.setBackgroundColor(GREEN);
    b.setAllCaps(false);
    b.setPadding(dp(14), dp(10), dp(14), dp(10));
    return b;
  }

  LinearLayout base(String title, String sub) {
    ScrollView sv = new ScrollView(this);
    sv.setFillViewport(true);
    sv.setBackgroundColor(BG);
    root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(18), dp(18), dp(18), dp(28));
    sv.addView(root, new ScrollView.LayoutParams(-1, -2));
    TextView h = text(title, 28, Color.WHITE);
    h.setTypeface(null, Typeface.BOLD);
    root.addView(h);
    if (sub != null) {
      TextView x = text(sub, 14, MUTED);
      root.addView(x);
    }
    setContentView(sv);
    return root;
  }

  void setupScreen() {
    base("Čkiletova tabla", "Create a tournament. Add 2–8 contestants to begin.");
    namesBox = new LinearLayout(this);
    namesBox.setOrientation(LinearLayout.VERTICAL);
    root.addView(namesBox);
    addNameField("");
    addNameField("");
    Button plus = button("＋  Add contestant");
    plus.setOnClickListener(
        v -> {
          if (namesBox.getChildCount() < 8) addNameField("");
          else toast("Maximum 8 contestants");
        });
    root.addView(plus, margin(0, 12));
    tournamentSettingsButton = subtleButton(tournamentSettingsSummary());
    tournamentSettingsButton.setOnClickListener(v -> tournamentSettingsDialog());
    root.addView(tournamentSettingsButton, margin(0, 6));
    Button confirm = button("CONFIRM CONTESTANTS");
    confirm.setOnClickListener(v -> confirmPlayers());
    root.addView(confirm, margin(0, 24));
    Space spacer = new Space(this);
    root.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1f));
    Button info = subtleButton("ⓘ");
    info.setTextSize(18);
    info.setContentDescription("Information");
    info.setOnClickListener(
        v ->
            new AlertDialog.Builder(this)
                .setTitle("Čkiletova tabla")
                .setMessage(
                    "Author: Vilim Hlusicka (vilim.hlusicka@gmail.com)\n\nVersion: "
                        + BuildConfig.VERSION_NAME
                        + "\n\nChanges in 1.1.6:"
                        + "\n• Added completed-tournament Excel export and sharing."
                        + "\n• The workbook contains the final table, tournament date, and all match results.")
                .setPositiveButton("OK", null)
                .show());
    LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(dp(52), dp(52));
    infoParams.gravity = Gravity.CENTER_HORIZONTAL;
    infoParams.setMargins(0, dp(6), 0, dp(20));
    root.addView(info, infoParams);
  }

  Button subtleButton(String label) {
    Button b = button(label);
    b.setTextColor(MUTED);
    b.setTextSize(12);
    b.setBackgroundColor(CARD);
    b.setAlpha(.72f);
    return b;
  }

  String tournamentSettingsSummary() {
    if (tournamentMode.isEmpty() || leagueMatchesPerPlayer < 1) {
      return "Select tournament format  (required)";
    }
    if (tournamentMode.equals("knockout")) {
      return "Tournament: League + knockout  •  "
          + leagueMatchesPerPlayer
          + " league matches/player  •  Top "
          + knockoutQualifiers;
    }
    return "Tournament: League  •  " + leagueMatchesPerPlayer + " matches/player";
  }

  void tournamentSettingsDialog() {
    LinearLayout content = new LinearLayout(this);
    content.setOrientation(LinearLayout.VERTICAL);
    content.setPadding(dp(20), 0, dp(20), 0);
    RadioGroup modes = new RadioGroup(this);
    RadioButton league = new RadioButton(this);
    league.setText("League only");
    league.setId(View.generateViewId());
    RadioButton knockout = new RadioButton(this);
    knockout.setText("League + knockout");
    knockout.setId(View.generateViewId());
    modes.addView(league);
    modes.addView(knockout);
    if (tournamentMode.equals("knockout")) modes.check(knockout.getId());
    else if (tournamentMode.equals("league")) modes.check(league.getId());
    content.addView(modes);
    TextView matchesLabel = dialogFieldLabel("Number of league matches per contestant");
    content.addView(matchesLabel);
    EditText matches = numberInput("League matches per contestant", leagueMatchesPerPlayer);
    content.addView(matches);
    TextView qualifiersLabel = dialogFieldLabel("Number of contestants proceeding to knockout");
    content.addView(qualifiersLabel);
    EditText qualifiers = numberInput("Contestants proceeding to knockout", knockoutQualifiers);
    content.addView(qualifiers);
    qualifiersLabel.setVisibility(tournamentMode.equals("knockout") ? View.VISIBLE : View.GONE);
    qualifiers.setVisibility(tournamentMode.equals("knockout") ? View.VISIBLE : View.GONE);
    modes.setOnCheckedChangeListener(
        (group, checked) -> {
          int visibility = checked == knockout.getId() ? View.VISIBLE : View.GONE;
          qualifiersLabel.setVisibility(visibility);
          qualifiers.setVisibility(visibility);
        });
    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setTitle("Tournament format")
            .setView(content)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create();
    dialog.setOnShowListener(
        ignored ->
            dialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                    v -> {
                      int selectedMode = modes.getCheckedRadioButtonId();
                      if (selectedMode != league.getId() && selectedMode != knockout.getId()) {
                        toast("Select League only or League + knockout");
                        return;
                      }
                      int matchCount = parsePositive(matches);
                      int qualifierCount = parsePositive(qualifiers);
                      boolean hasKnockout = selectedMode == knockout.getId();
                      if (matchCount < 1 || matchCount > 100) {
                        matches.setError("Enter a number from 1 to 100");
                        return;
                      }
                      if (hasKnockout
                          && (qualifierCount < 2
                              || qualifierCount > 8
                              || qualifierCount % 2 != 0)) {
                        qualifiers.setError("Enter an even number from 2 to 8");
                        return;
                      }
                      tournamentMode = hasKnockout ? "knockout" : "league";
                      leagueMatchesPerPlayer = matchCount;
                      if (hasKnockout) knockoutQualifiers = qualifierCount;
                      tournamentSettingsButton.setText(tournamentSettingsSummary());
                      dialog.dismiss();
                    }));
    dialog.show();
  }

  EditText numberInput(String hint, int value) {
    EditText input = new EditText(this);
    input.setHint(hint);
    if (value > 0) input.setText(String.valueOf(value));
    input.setSingleLine();
    input.setInputType(InputType.TYPE_CLASS_NUMBER);
    return input;
  }

  TextView dialogFieldLabel(String label) {
    TextView view = new TextView(this);
    view.setText(label);
    view.setTextSize(12);
    view.setTextColor(Color.DKGRAY);
    view.setTypeface(null, Typeface.BOLD);
    view.setPadding(0, dp(12), 0, 0);
    return view;
  }

  int parsePositive(EditText input) {
    try {
      return Integer.parseInt(input.getText().toString());
    } catch (NumberFormatException ignored) {
      return -1;
    }
  }

  void addNameField(String name) {
    LinearLayout line = new LinearLayout(this);
    line.setOrientation(LinearLayout.VERTICAL);
    line.setPadding(0, dp(4), 0, dp(4));
    EditText e = new EditText(this);
    e.setHint("Contestant " + (namesBox.getChildCount() + 1));
    e.setText(name);
    e.setTextColor(Color.WHITE);
    e.setHintTextColor(MUTED);
    e.setSingleLine();
    e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(GREEN));
    e.setPadding(dp(12), dp(12), dp(12), dp(12));
    line.addView(e, new LinearLayout.LayoutParams(-1, -2));
    Button assign = subtleButton("＋ Assign team");
    assign.setTextSize(12);
    assign.setTag("");
    assign.setOnClickListener(v -> assignTeam(assign));
    LinearLayout.LayoutParams assignParams = new LinearLayout.LayoutParams(-2, -2);
    assignParams.gravity = Gravity.END;
    assignParams.setMargins(0, dp(2), 0, dp(2));
    line.addView(assign, assignParams);
    namesBox.addView(line, margin(0, 4));
  }

  void assignTeam(Button assign) {
    EditText input = new EditText(this);
    input.setHint("Football team name");
    input.setSingleLine();
    input.setText(String.valueOf(assign.getTag()));
    input.setSelectAllOnFocus(false);
    int pad = dp(20);
    FrameLayout box = new FrameLayout(this);
    box.setPadding(pad, 0, pad, 0);
    box.addView(input, new FrameLayout.LayoutParams(-1, -2));
    new AlertDialog.Builder(this)
        .setTitle("Assign football team")
        .setView(box)
        .setNegativeButton("Cancel", null)
        .setNeutralButton(
            "Clear",
            (d, w) -> {
              assign.setTag("");
              assign.setText("＋ Assign team");
            })
        .setPositiveButton(
            "Save",
            (d, w) -> {
              String team = input.getText().toString().trim();
              assign.setTag(team);
              assign.setText(team.isEmpty() ? "＋ Assign team" : "Team: " + team);
            })
        .show();
  }

  void confirmPlayers() {
    if (tournamentMode.isEmpty() || leagueMatchesPerPlayer < 1) {
      toast("Select the required tournament format and number of matches");
      return;
    }
    if (tournamentMode.equals("knockout") && knockoutQualifiers < 2) {
      toast("Select the required number of knockout contestants");
      return;
    }
    ArrayList<String> n = new ArrayList<>(), t = new ArrayList<>();
    for (int i = 0; i < namesBox.getChildCount(); i++) {
      LinearLayout line = (LinearLayout) namesBox.getChildAt(i);
      String s = ((EditText) line.getChildAt(0)).getText().toString().trim();
      if (!s.isEmpty()) {
        n.add(s);
        t.add(String.valueOf(line.getChildAt(1).getTag()).trim());
      }
    }
    if (n.size() < 2) {
      toast("Enter at least 2 contestants");
      return;
    }
    HashSet<String> uniq = new HashSet<>();
    for (String s : n)
      if (!uniq.add(s.toLowerCase(Locale.ROOT))) {
        toast("Contestant names must be unique");
        return;
      }
    if ((n.size() * leagueMatchesPerPlayer) % 2 != 0) {
      toast("This player count requires an even number of league matches per contestant");
      return;
    }
    if (tournamentMode.equals("knockout") && knockoutQualifiers > n.size()) {
      toast("Knockout qualifiers cannot exceed the number of contestants");
      return;
    }
    players.clear();
    players.addAll(n);
    teams.clear();
    teams.addAll(t);
    queue.clear();
    games.clear();
    knockoutAdvancers.clear();
    phase = "league";
    tournamentDateMillis = System.currentTimeMillis();
    refillQueue();
    save();
    tableScreen();
  }

  void tableScreen() {
    String phaseLabel =
        phase.equals("knockout")
            ? "Knockout stage"
            : phase.equals("knockout_ready")
                ? "League stage complete"
                : phase.equals("finished") ? "Tournament finished" : "League stage";
    base(
        "League table",
        phaseLabel + "  •  " + leagueGameCount() + " league games played  •  3 points for a win");
    String nextLabel;
    if (phase.equals("finished")) nextLabel = "Export results...";
    else if (phase.equals("knockout_ready")) nextLabel = "GO TO KNOCKOUT  ›";
    else if (phase.equals("knockout")) nextLabel = "PLAY NEXT KNOCKOUT MATCH  ›";
    else nextLabel = "PLAY GAME " + (games.size() + 1) + "  ›";
    Button next = button(nextLabel);
    next.setOnClickListener(
        v -> {
          if (phase.equals("finished")) {
            exportResults();
          } else if (phase.equals("knockout_ready")) {
            startKnockout();
            save();
            tableScreen();
          } else {
            matchScreen(false);
          }
        });
    root.addView(next, margin(0, 8));
    Button all = button("All matches");
    all.setOnClickListener(v -> gamesScreen(-1));
    root.addView(all, margin(0, 4));
    if ((phase.equals("knockout") || phase.equals("finished"))
        && !knockoutBracketRounds.isEmpty()) {
      root.addView(knockoutTree(), margin(0, 18));
    } else {
      root.addView(leagueTable(), margin(0, 18));
    }
    Button reset = button("Reset tournament");
    reset.setBackgroundColor(Color.rgb(224, 92, 92));
    reset.setOnClickListener(v -> resetDialog());
    root.addView(reset, margin(0, 18));
  }

  View leagueTable() {
    LinearLayout table = new LinearLayout(this);
    table.setOrientation(LinearLayout.VERTICAL);
    table.setBackgroundColor(CARD);
    table.addView(row(new String[] {"#  PLAYER", "PT", "P", "W", "D", "L", "GD"}, true, null));
    ArrayList<Integer> order = standingsOrder();
    int winnerId = order.isEmpty() ? -1 : order.get(0);
    for (int p = 0; p < order.size(); p++) {
      int id = order.get(p), rank = p + 1;
      int[] s = stats(id);
      String gd = (s[4] > 0 ? "+" : "") + s[4], team = id < teams.size() ? teams.get(id) : "";
      String label = rank + "  " + players.get(id) + (team.isEmpty() ? "" : "\n     " + team);
      View playerRow =
          row(
              new String[] {
                label, "" + s[0], "" + (s[1] + s[2] + s[3]), "" + s[1], "" + s[2], "" + s[3], gd
              },
              false,
              v -> gamesScreen(id));
      if (phase.equals("finished") && tournamentMode.equals("league") && id == winnerId) {
        ((TextView) ((LinearLayout) playerRow).getChildAt(0)).setTextColor(GREEN);
      }
      if (phase.equals("knockout_ready")
          && tournamentMode.equals("knockout")
          && p >= knockoutQualifiers) {
        playerRow.setAlpha(.42f);
      }
      table.addView(playerRow);
    }
    return table;
  }

  View knockoutTree() {
    HorizontalScrollView scroll = new HorizontalScrollView(this);
    scroll.setFillViewport(true);
    LinearLayout rounds = new LinearLayout(this);
    rounds.setOrientation(LinearLayout.HORIZONTAL);
    rounds.setPadding(dp(4), dp(4), dp(4), dp(4));
    for (int roundIndex = 0; roundIndex < knockoutBracketRounds.size(); roundIndex++) {
      ArrayList<String> fixtures = knockoutBracketRounds.get(roundIndex);
      LinearLayout column = new LinearLayout(this);
      column.setOrientation(LinearLayout.VERTICAL);
      column.setPadding(dp(5), 0, dp(5), 0);
      TextView heading = text(knockoutRoundLabel(roundIndex, fixtures.size()), 14, GREEN);
      heading.setTypeface(null, Typeface.BOLD);
      heading.setGravity(Gravity.CENTER);
      column.addView(heading);
      for (String fixture : fixtures) {
        column.addView(knockoutFixtureCard(roundIndex, fixture), margin(0, 6));
      }
      rounds.addView(column, new LinearLayout.LayoutParams(dp(230), -2));
    }
    scroll.addView(rounds, new HorizontalScrollView.LayoutParams(-2, -2));
    return scroll;
  }

  String knockoutRoundLabel(int roundIndex, int fixtureCount) {
    if (fixtureCount == 1) return "FINAL";
    if (fixtureCount == 2) return "SEMIFINALS";
    return "ROUND " + (roundIndex + 1);
  }

  View knockoutFixtureCard(int roundIndex, String fixture) {
    String[] pair = fixture.split(":");
    int first = Integer.parseInt(pair[0]), second = Integer.parseInt(pair[1]);
    LinearLayout card = new LinearLayout(this);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setBackgroundColor(CARD);
    Game result = knockoutResult(roundIndex, first, second);
    TextView firstLine = text(players.get(first) + scoreSuffix(result, true), 14, Color.WHITE);
    firstLine.setTypeface(null, Typeface.BOLD);
    card.addView(firstLine);
    if (second < 0) {
      card.addView(text("BYE", 12, MUTED));
    } else {
      TextView secondLine = text(players.get(second) + scoreSuffix(result, false), 14, Color.WHITE);
      secondLine.setTypeface(null, Typeface.BOLD);
      if (result != null) {
        if (result.ga > result.gb) secondLine.setAlpha(.4f);
        else firstLine.setAlpha(.4f);
      }
      card.addView(secondLine);
    }
    return card;
  }

  String scoreSuffix(Game result, boolean first) {
    if (result == null) return "";
    return "    " + (first ? result.ga : result.gb);
  }

  Game knockoutResult(int roundIndex, int first, int second) {
    for (Game game : games) {
      if (game.knockout && game.knockoutRound == roundIndex && game.a == first && game.b == second)
        return game;
    }
    return null;
  }

  ArrayList<Integer> standingsOrder() {
    ArrayList<Integer> order = new ArrayList<>();
    for (int i = 0; i < players.size(); i++) order.add(i);
    order.sort(
        (x, y) -> {
          int[] X = stats(x), Y = stats(y);
          int c = Integer.compare(Y[0], X[0]);
          if (c == 0) c = Integer.compare(Y[4], X[4]);
          if (c == 0) c = Integer.compare(Y[5], X[5]);
          return c;
        });
    return order;
  }

  int leagueGameCount() {
    int count = 0;
    for (Game game : games) if (!game.knockout) count++;
    return count;
  }

  int knockoutGameCount() {
    int count = 0;
    for (Game game : games) if (game.knockout) count++;
    return count;
  }

  View row(String[] vals, boolean header, View.OnClickListener click) {
    LinearLayout r = new LinearLayout(this);
    r.setOrientation(LinearLayout.HORIZONTAL);
    r.setPadding(dp(2), dp(5), dp(2), dp(5));
    boolean twoLines = !header && vals[0].contains("\n");
    for (int i = 0; i < vals.length; i++) {
      TextView t = text(vals[i], header ? 10 : 13, i == 0 ? Color.WHITE : MUTED);
      if (twoLines && i == 0) {
        SpannableString styled = new SpannableString(vals[i]);
        int split = vals[i].indexOf('\n') + 1;
        styled.setSpan(
            new ForegroundColorSpan(Color.argb(128, 255, 255, 255)),
            split,
            styled.length(),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        t.setText(styled);
      }
      if (header || i == 1) t.setTypeface(null, Typeface.BOLD);
      t.setGravity(i == 0 ? Gravity.CENTER_VERTICAL : Gravity.CENTER);
      r.addView(t, new LinearLayout.LayoutParams(0, dp(twoLines ? 64 : 48), i == 0 ? 3.4f : 1f));
    }
    if (click != null) {
      r.setClickable(true);
      r.setOnClickListener(click);
    }
    return r;
  }

  int[] stats(int p) {
    int pts = 0, w = 0, d = 0, l = 0, gd = 0, gf = 0;
    for (Game g : games) {
      if (g.knockout) continue;
      if (g.a != p && g.b != p) continue;
      int mine = g.a == p ? g.ga : g.gb, other = g.a == p ? g.gb : g.ga;
      gf += mine;
      gd += mine - other;
      if (mine > other) {
        w++;
        pts += 3;
      } else if (mine == other) {
        d++;
        pts++;
      } else l++;
    }
    return new int[] {pts, w, d, l, gd, gf};
  }

  void matchScreen(boolean editing) {
    int ai, bi;
    if (editing) {
      Game g = games.get(games.size() - 1);
      ai = g.a;
      bi = g.b;
    } else {
      String[] q = queue.get(0).split(":");
      ai = Integer.parseInt(q[0]);
      bi = Integer.parseInt(q[1]);
    }
    boolean displayedKnockout =
        editing ? games.get(games.size() - 1).knockout : phase.equals("knockout");
    base(
        editing ? "Correct latest result" : "Game " + (games.size() + 1),
        editing
            ? "Only the latest match can be edited."
            : phase.equals("knockout")
                ? "Knockout match • a winner is required"
                : "Next league fixture");
    TextView versus =
        text(
            displayedKnockout
                ? players.get(ai) + "\n\nVS\n\n" + players.get(bi)
                : "HOME\n" + players.get(ai) + "\n\nVS\n\n" + players.get(bi) + "\nAWAY",
            25,
            Color.WHITE);
    versus.setGravity(Gravity.CENTER);
    versus.setTypeface(null, Typeface.BOLD);
    root.addView(versus, margin(0, 20));
    LinearLayout scores = new LinearLayout(this);
    scores.setGravity(Gravity.CENTER);
    EditText a = score();
    EditText b = score();
    scores.addView(a, new LinearLayout.LayoutParams(dp(92), dp(72)));
    scores.addView(text("  —  ", 28, MUTED));
    scores.addView(b, new LinearLayout.LayoutParams(dp(92), dp(72)));
    root.addView(scores);
    if (editing) {
      Game g = games.get(games.size() - 1);
      a.setText("" + g.ga);
      b.setText("" + g.gb);
    }
    Button finish = button(editing ? "SAVE CORRECTION" : "FINISH MATCH");
    int fa = ai, fb = bi;
    finish.setOnClickListener(
        v -> {
          if (a.getText().length() == 0 || b.getText().length() == 0) {
            toast("Enter both scores");
            return;
          }
          int ga = Integer.parseInt(a.getText().toString()),
              gb = Integer.parseInt(b.getText().toString());
          boolean knockoutMatch =
              editing ? games.get(games.size() - 1).knockout : phase.equals("knockout");
          if (knockoutMatch && ga == gb) {
            toast("A knockout match cannot end in a draw");
            return;
          }
          new AlertDialog.Builder(this)
              .setTitle(editing ? "Save corrected result?" : "Finish this match?")
              .setMessage(players.get(fa) + "  " + ga + " – " + gb + "  " + players.get(fb))
              .setNegativeButton("Cancel", null)
              .setPositiveButton(
                  "Confirm",
                  (d, x) -> {
                    if (editing) {
                      Game g = games.get(games.size() - 1);
                      int oldWinner = g.ga > g.gb ? g.a : g.b;
                      g.ga = ga;
                      g.gb = gb;
                      if (g.knockout) replaceLatestKnockoutWinner(oldWinner, ga > gb ? g.a : g.b);
                      else if (phase.equals("knockout") && knockoutGameCount() == 0) {
                        queue.clear();
                        knockoutAdvancers.clear();
                        phase = "league";
                        startKnockout();
                      }
                    } else {
                      boolean knockout = phase.equals("knockout");
                      games.add(
                          new Game(fa, fb, ga, gb, knockout, knockout ? currentKnockoutRound : -1));
                      queue.remove(0);
                      if (knockout) knockoutAdvancers.add(ga > gb ? fa : fb);
                      if (queue.isEmpty()) completeCurrentStage();
                    }
                    save();
                    tableScreen();
                  })
              .show();
        });
    root.addView(finish, margin(0, 24));
    Button back = button("Cancel");
    back.setOnClickListener(v -> tableScreen());
    root.addView(back, margin(0, 8));
  }

  EditText score() {
    EditText e = new EditText(this);
    e.setTextColor(Color.WHITE);
    e.setTextSize(28);
    e.setGravity(Gravity.CENTER);
    e.setHint("0");
    e.setHintTextColor(MUTED);
    e.setInputType(InputType.TYPE_CLASS_NUMBER);
    e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(GREEN));
    return e;
  }

  void gamesScreen(int player) {
    String title = player < 0 ? "All matches" : players.get(player);
    base(
        title,
        player < 0
            ? "Complete tournament history • league home team is shown first"
            : "Previously played matches • league home team is shown first");
    if (games.isEmpty()) root.addView(text("No matches played yet.", 16, MUTED));
    for (int i = games.size() - 1; i >= 0; i--) {
      Game g = games.get(i);
      if (player >= 0 && g.a != player && g.b != player) continue;
      LinearLayout card = new LinearLayout(this);
      card.setOrientation(LinearLayout.VERTICAL);
      card.setBackgroundColor(CARD);
      TextView n =
          text(
              "GAME "
                  + (i + 1)
                  + "  •  "
                  + (g.knockout ? "KNOCKOUT" : "LEAGUE")
                  + (g.knockout ? "" : "  •  HOME / AWAY"),
              11,
              GREEN);
      n.setTypeface(null, Typeface.BOLD);
      card.addView(n);
      TextView result =
          text(
              g.knockout
                  ? players.get(g.a) + "   " + g.ga + " – " + g.gb + "   " + players.get(g.b)
                  : "HOME  "
                      + players.get(g.a)
                      + "   "
                      + g.ga
                      + " – "
                      + g.gb
                      + "   "
                      + players.get(g.b)
                      + "  AWAY",
              17,
              Color.WHITE);
      result.setTypeface(null, Typeface.BOLD);
      card.addView(result);
      if (i == games.size() - 1) {
        Button edit = button("Edit latest result");
        edit.setOnClickListener(v -> matchScreen(true));
        card.addView(edit, margin(8, 5));
      }
      root.addView(card, margin(0, 6));
    }
    Button back = button("‹  Back to table");
    back.setOnClickListener(v -> tableScreen());
    root.addView(back, margin(0, 20));
  }

  void exportResults() {
    try {
      shareResultsWorkbook(createResultsWorkbook());
    } catch (IOException error) {
      toast("Could not create the Excel workbook");
    } catch (ActivityNotFoundException error) {
      toast("No sharing app is available");
    }
  }

  File createResultsWorkbook() throws IOException {
    File directory = new File(getCacheDir(), "exports");
    if (!directory.exists() && !directory.mkdirs()) {
      throw new IOException("Cannot create export directory");
    }
    File workbook = new File(directory, "Ckiletova-tabla-results.xlsx");
    try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(workbook))) {
      writeZipEntry(
          zip,
          "[Content_Types].xml",
          "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
              + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
              + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
              + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
              + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
              + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
              + "<Override PartName=\"/xl/worksheets/sheet2.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
              + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
              + "</Types>");
      writeZipEntry(
          zip,
          "_rels/.rels",
          "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
              + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
              + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
              + "</Relationships>");
      writeZipEntry(
          zip,
          "xl/workbook.xml",
          "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
              + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
              + "<sheets><sheet name=\"Table\" sheetId=\"1\" r:id=\"rId1\"/>"
              + "<sheet name=\"Results\" sheetId=\"2\" r:id=\"rId2\"/></sheets></workbook>");
      writeZipEntry(
          zip,
          "xl/_rels/workbook.xml.rels",
          "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
              + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
              + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
              + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet2.xml\"/>"
              + "<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
              + "</Relationships>");
      writeZipEntry(zip, "xl/styles.xml", workbookStylesXml());
      writeZipEntry(zip, "xl/worksheets/sheet1.xml", tableSheetXml());
      writeZipEntry(zip, "xl/worksheets/sheet2.xml", resultsSheetXml());
    }
    return workbook;
  }

  String workbookStylesXml() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
        + "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font>"
        + "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>"
        + "<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill></fills>"
        + "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"
        + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
        + "<cellXfs count=\"2\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
        + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/></cellXfs>"
        + "</styleSheet>";
  }

  String tableSheetXml() {
    ArrayList<String[]> rows = new ArrayList<>();
    String date = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date(tournamentDateMillis));
    rows.add(new String[] {"Tournament date: " + date});
    rows.add(new String[] {});
    rows.add(new String[] {"#  PLAYER", "PT", "P", "W", "D", "L", "GD"});
    ArrayList<Integer> order = standingsOrder();
    for (int rank = 0; rank < order.size(); rank++) {
      int id = order.get(rank);
      int[] stats = stats(id);
      String team = id < teams.size() ? teams.get(id) : "";
      String player = (rank + 1) + "  " + players.get(id) + (team.isEmpty() ? "" : "\n     " + team);
      rows.add(
          new String[] {
            player,
            String.valueOf(stats[0]),
            String.valueOf(stats[1] + stats[2] + stats[3]),
            String.valueOf(stats[1]),
            String.valueOf(stats[2]),
            String.valueOf(stats[3]),
            (stats[4] > 0 ? "+" : "") + stats[4]
          });
    }
    return sheetXml(rows, 3, true);
  }

  String resultsSheetXml() {
    ArrayList<String[]> rows = new ArrayList<>();
    rows.add(new String[] {"GAME", "TYPE", "HOME / CONTESTANT 1", "AWAY / CONTESTANT 2", "RESULT"});
    for (int i = 0; i < games.size(); i++) {
      Game game = games.get(i);
      rows.add(
          new String[] {
            String.valueOf(i + 1),
            game.knockout ? "KNOCKOUT" : "LEAGUE",
            players.get(game.a),
            players.get(game.b),
            game.ga + " – " + game.gb
          });
    }
    return sheetXml(rows, 1, false);
  }

  String sheetXml(ArrayList<String[]> rows, int headerRow, boolean mergeDate) {
    StringBuilder xml =
        new StringBuilder(
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                + "<cols><col min=\"1\" max=\"1\" width=\"28\" customWidth=\"1\"/>"
                + "<col min=\"2\" max=\"7\" width=\"18\" customWidth=\"1\"/></cols><sheetData>");
    for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
      String[] row = rows.get(rowIndex);
      int number = rowIndex + 1;
      xml.append("<row r=\"").append(number).append("\">");
      for (int column = 0; column < row.length; column++) {
        String ref = columnName(column) + number;
        int style = number == headerRow || (mergeDate && number == 1) ? 1 : 0;
        xml.append("<c r=\"").append(ref).append("\" t=\"inlineStr\" s=\"").append(style).append("\"><is><t xml:space=\"preserve\">")
            .append(xmlEscape(row[column]))
            .append("</t></is></c>");
      }
      xml.append("</row>");
    }
    xml.append("</sheetData>");
    if (mergeDate) xml.append("<mergeCells count=\"1\"><mergeCell ref=\"A1:G1\"/></mergeCells>");
    return xml.append("</worksheet>").toString();
  }

  String columnName(int index) {
    return String.valueOf((char) ('A' + index));
  }

  String xmlEscape(String value) {
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
  }

  void writeZipEntry(ZipOutputStream zip, String name, String content) throws IOException {
    zip.putNextEntry(new ZipEntry(name));
    zip.write(content.getBytes("UTF-8"));
    zip.closeEntry();
  }

  void shareResultsWorkbook(File workbook) {
    Uri uri = Uri.parse("content://" + getPackageName() + ".exports/" + workbook.getName());
    Intent share = new Intent(Intent.ACTION_SEND);
    share.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    share.putExtra(Intent.EXTRA_SUBJECT, "Čkiletova tabla tournament results");
    share.putExtra(Intent.EXTRA_TEXT, "The completed tournament results are attached.");
    share.putExtra(Intent.EXTRA_STREAM, uri);
    share.setClipData(ClipData.newUri(getContentResolver(), workbook.getName(), uri));
    share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    startActivity(Intent.createChooser(share, "Share tournament results"));
  }

  void resetDialog() {
    AlertDialog d =
        new AlertDialog.Builder(this)
            .setTitle("Reset the whole tournament?")
            .setMessage(
                "All contestants and match results will be erased.\n\nPlease wait 10 seconds.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("YES (10)", null)
            .create();
    d.setOnShowListener(
        x -> {
          Button yes = d.getButton(-1);
          yes.setEnabled(false);
          new CountDownTimer(10000, 1000) {
            public void onTick(long m) {
              yes.setText("YES (" + (m / 1000 + 1) + ")");
            }

            public void onFinish() {
              yes.setText("YES, RESET");
              yes.setEnabled(true);
              yes.setOnClickListener(
                  v -> {
                    players.clear();
                    teams.clear();
                    games.clear();
                    queue.clear();
                    knockoutAdvancers.clear();
                    knockoutBracketRounds.clear();
                    currentKnockoutRound = -1;
                    tournamentDateMillis = 0L;
                    tournamentMode = "";
                    phase = "setup";
                    leagueMatchesPerPlayer = 0;
                    knockoutQualifiers = 0;
                    prefs.edit().clear().apply();
                    d.dismiss();
                    setupScreen();
                  });
            }
          }.start();
        });
    d.show();
  }

  void refillQueue() {
    queue.clear();
    int playerCount = players.size();
    int[] homeBalance = new int[playerCount];
    if (playerCount % 2 == 0) {
      int roundsRemaining = leagueMatchesPerPlayer;
      while (roundsRemaining > 0) {
        ArrayList<Integer> rotation = shuffledPlayers(false);
        int cycleRounds = Math.min(playerCount - 1, roundsRemaining);
        appendCircleRounds(rotation, cycleRounds, homeBalance);
        roundsRemaining -= cycleRounds;
      }
    } else {
      int fullCycles = leagueMatchesPerPlayer / (playerCount - 1);
      int remainder = leagueMatchesPerPlayer % (playerCount - 1);
      for (int cycle = 0; cycle < fullCycles; cycle++) {
        appendCircleRounds(shuffledPlayers(true), playerCount, homeBalance);
      }
      if (remainder > 0) appendOddRegularFixtures(remainder, homeBalance);
    }
  }

  ArrayList<Integer> shuffledPlayers(boolean includeBye) {
    ArrayList<Integer> rotation = new ArrayList<>();
    for (int i = 0; i < players.size(); i++) rotation.add(i);
    Collections.shuffle(rotation);
    if (includeBye) rotation.add(-1);
    return rotation;
  }

  void appendCircleRounds(ArrayList<Integer> rotation, int rounds, int[] homeBalance) {
    int count = rotation.size();
    for (int round = 0; round < rounds; round++) {
      for (int i = 0; i < count / 2; i++) {
        int left = rotation.get(i), right = rotation.get(count - 1 - i);
        if (left >= 0 && right >= 0) addBalancedFixture(left, right, homeBalance);
      }
      int last = rotation.remove(count - 1);
      rotation.add(1, last);
    }
  }

  void appendOddRegularFixtures(int degree, int[] homeBalance) {
    ArrayList<Integer> order = shuffledPlayers(false);
    int count = order.size();
    for (int step = 1; step <= degree / 2; step++) {
      for (int pass = 0; pass < 2; pass++) {
        for (int i = pass; i < count; i += 2) {
          addBalancedFixture(order.get(i), order.get((i + step) % count), homeBalance);
        }
      }
    }
  }

  void addBalancedFixture(int first, int second, int[] homeBalance) {
    boolean firstHome =
        homeBalance[first] < homeBalance[second]
            || (homeBalance[first] == homeBalance[second] && queue.size() % 2 == 0);
    int home = firstHome ? first : second;
    int away = firstHome ? second : first;
    homeBalance[home]++;
    homeBalance[away]--;
    queue.add(home + ":" + away);
  }

  void completeCurrentStage() {
    if (phase.equals("league")) {
      if (tournamentMode.equals("knockout")) phase = "knockout_ready";
      else phase = "finished";
    } else if (phase.equals("knockout")) {
      advanceKnockout();
    }
  }

  void startKnockout() {
    phase = "knockout";
    currentKnockoutRound = 0;
    knockoutAdvancers.clear();
    knockoutBracketRounds.clear();
    ArrayList<Integer> qualifiers = standingsOrder();
    while (qualifiers.size() > knockoutQualifiers) qualifiers.remove(qualifiers.size() - 1);
    int bracketSize = 1;
    while (bracketSize < qualifiers.size()) bracketSize *= 2;
    int byes = bracketSize - qualifiers.size();
    ArrayList<String> firstRound = new ArrayList<>();
    for (int i = 0; i < byes; i++) {
      knockoutAdvancers.add(qualifiers.get(i));
      firstRound.add(qualifiers.get(i) + ":-1");
    }
    for (int left = byes, right = qualifiers.size() - 1; left < right; left++, right--) {
      String fixture = qualifiers.get(left) + ":" + qualifiers.get(right);
      queue.add(fixture);
      firstRound.add(fixture);
    }
    knockoutBracketRounds.add(firstRound);
    if (queue.isEmpty()) advanceKnockout();
  }

  void advanceKnockout() {
    if (knockoutAdvancers.size() == 1) {
      phase = "finished";
      return;
    }
    ArrayList<Integer> participants = new ArrayList<>(knockoutAdvancers);
    knockoutAdvancers.clear();
    currentKnockoutRound++;
    ArrayList<String> nextRound = new ArrayList<>();
    for (int left = 0, right = participants.size() - 1; left < right; left++, right--) {
      String fixture = participants.get(left) + ":" + participants.get(right);
      queue.add(fixture);
      nextRound.add(fixture);
    }
    knockoutBracketRounds.add(nextRound);
  }

  void replaceLatestKnockoutWinner(int oldWinner, int newWinner) {
    if (oldWinner == newWinner) return;
    for (int i = knockoutAdvancers.size() - 1; i >= 0; i--) {
      if (knockoutAdvancers.get(i) == oldWinner) {
        knockoutAdvancers.set(i, newWinner);
        return;
      }
    }
    for (int i = 0; i < queue.size(); i++) {
      String[] pair = queue.get(i).split(":");
      int home = Integer.parseInt(pair[0]), away = Integer.parseInt(pair[1]);
      if (home == oldWinner) home = newWinner;
      if (away == oldWinner) away = newWinner;
      queue.set(i, home + ":" + away);
    }
    if (currentKnockoutRound >= 0 && currentKnockoutRound < knockoutBracketRounds.size()) {
      ArrayList<String> fixtures = knockoutBracketRounds.get(currentKnockoutRound);
      for (int i = 0; i < fixtures.size(); i++) {
        String[] pair = fixtures.get(i).split(":");
        int first = Integer.parseInt(pair[0]), second = Integer.parseInt(pair[1]);
        if (first == oldWinner) first = newWinner;
        if (second == oldWinner) second = newWinner;
        fixtures.set(i, first + ":" + second);
      }
    }
  }

  void save() {
    try {
      JSONObject o = new JSONObject();
      o.put("schedulerVersion", 4);
      o.put("tournamentMode", tournamentMode);
      o.put("phase", phase);
      o.put("leagueMatchesPerPlayer", leagueMatchesPerPlayer);
      o.put("knockoutQualifiers", knockoutQualifiers);
      o.put("currentKnockoutRound", currentKnockoutRound);
      o.put("tournamentDateMillis", tournamentDateMillis);
      JSONArray p = new JSONArray();
      for (String s : players) p.put(s);
      o.put("players", p);
      JSONArray ts = new JSONArray();
      for (String s : teams) ts.put(s);
      o.put("teams", ts);
      JSONArray gs = new JSONArray();
      for (Game g : games)
        gs.put(
            new JSONArray()
                .put(g.a)
                .put(g.b)
                .put(g.ga)
                .put(g.gb)
                .put(g.knockout)
                .put(g.knockoutRound));
      o.put("games", gs);
      JSONArray q = new JSONArray();
      for (String s : queue) q.put(s);
      o.put("queue", q);
      JSONArray advancers = new JSONArray();
      for (int id : knockoutAdvancers) advancers.put(id);
      o.put("knockoutAdvancers", advancers);
      JSONArray bracket = new JSONArray();
      for (ArrayList<String> round : knockoutBracketRounds) {
        JSONArray savedRound = new JSONArray();
        for (String fixture : round) savedRound.put(fixture);
        bracket.put(savedRound);
      }
      o.put("knockoutBracketRounds", bracket);
      prefs.edit().putString("data", o.toString()).apply();
    } catch (Exception ignored) {
    }
  }

  void load() {
    try {
      String raw = prefs.getString("data", null);
      if (raw == null) return;
      JSONObject o = new JSONObject(raw);
      boolean needsTournamentDateMigration = !o.has("tournamentDateMillis");
      int schedulerVersion = o.optInt("schedulerVersion", 1);
      tournamentMode = o.optString("tournamentMode", "league");
      phase = o.optString("phase", "league");
      leagueMatchesPerPlayer = o.optInt("leagueMatchesPerPlayer", 2);
      knockoutQualifiers = o.optInt("knockoutQualifiers", 2);
      currentKnockoutRound = o.optInt("currentKnockoutRound", -1);
      tournamentDateMillis = o.optLong("tournamentDateMillis", 0L);
      if (tournamentDateMillis == 0L) tournamentDateMillis = System.currentTimeMillis();
      JSONArray p = o.getJSONArray("players");
      for (int i = 0; i < p.length(); i++) players.add(p.getString(i));
      JSONArray ts = o.optJSONArray("teams");
      for (int i = 0; i < players.size(); i++)
        teams.add(ts != null && i < ts.length() ? ts.optString(i, "") : "");
      JSONArray gs = o.getJSONArray("games");
      for (int i = 0; i < gs.length(); i++) {
        JSONArray g = gs.getJSONArray(i);
        games.add(
            new Game(
                g.getInt(0),
                g.getInt(1),
                g.getInt(2),
                g.getInt(3),
                g.length() > 4 && g.optBoolean(4, false),
                g.length() > 5 ? g.optInt(5, -1) : -1));
      }
      if (schedulerVersion >= 2) {
        JSONArray q = o.optJSONArray("queue");
        if (q != null) for (int i = 0; i < q.length(); i++) queue.add(q.getString(i));
      }
      JSONArray advancers = o.optJSONArray("knockoutAdvancers");
      if (advancers != null)
        for (int i = 0; i < advancers.length(); i++) knockoutAdvancers.add(advancers.getInt(i));
      JSONArray bracket = o.optJSONArray("knockoutBracketRounds");
      if (bracket != null) {
        for (int roundIndex = 0; roundIndex < bracket.length(); roundIndex++) {
          JSONArray savedRound = bracket.getJSONArray(roundIndex);
          ArrayList<String> round = new ArrayList<>();
          for (int i = 0; i < savedRound.length(); i++) round.add(savedRound.getString(i));
          knockoutBracketRounds.add(round);
        }
      }
      if (schedulerVersion < 3 && players.size() >= 2) {
        leagueMatchesPerPlayer = 2 * (players.size() - 1);
        if (schedulerVersion < 2) {
          queue.clear();
          refillQueue();
        }
        phase = queue.isEmpty() && !games.isEmpty() ? "finished" : "league";
        save();
      } else if (needsTournamentDateMigration) save();
    } catch (Exception e) {
      players.clear();
      teams.clear();
      games.clear();
      queue.clear();
      knockoutAdvancers.clear();
      knockoutBracketRounds.clear();
    }
  }

  LinearLayout.LayoutParams margin(int h, int v) {
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.setMargins(dp(h), dp(v), dp(h), dp(v));
    return p;
  }

  int dp(int n) {
    return (int) (n * getResources().getDisplayMetrics().density + .5f);
  }

  void toast(String s) {
    Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onBackPressed() {
    if (players.size() >= 2) tableScreen();
    else super.onBackPressed();
  }
}
