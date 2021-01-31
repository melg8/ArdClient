package haven;

import haven.sloth.util.ObservableMap;
import haven.sloth.util.ObservableMapListener;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public class CustomWidgetList extends WidgetList<CustomWidgetList.Item> implements ObservableMapListener<String, Boolean> {
    public final ObservableMap<String, Boolean> customlist;
    public final String jsonname;

    public static final Comparator<Item> ITEM_COMPARATOR = Comparator.comparing(o -> o.name);

    public CustomWidgetList(ObservableMap<String, Boolean> list, String jsonname) {
        super(new Coord(200, 25), 10);
        customlist = list;
        customlist.addListener(this);
        this.jsonname = jsonname;
    }

    @SuppressWarnings("SynchronizeOnNonFinalField")
    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        switch (msg) {
            case "changed": {
                String name = (String) args[0];
                boolean val = (Boolean) args[1];
                synchronized (customlist) {
                    customlist.put(name, val);
                }
                Utils.saveCustomList(customlist, jsonname);
                break;
            }
            case "delete": {
                String name = (String) args[0];
                synchronized (customlist) {
                    customlist.remove(name);
                }
                Utils.saveCustomList(customlist, jsonname);
                removeitem((Item) sender, true);
                update();
                break;
            }
            default:
                super.wdgmsg(sender, msg, args);
                break;
        }
    }

    @SuppressWarnings("SynchronizeOnNonFinalField")
    public void add(String name) {
        if (name != null && !name.isEmpty() && !customlist.containsKey(name)) {
            synchronized (customlist) {
                customlist.put(name, true);
            }
            Utils.saveCustomList(customlist, jsonname);
            additem(new Item(name));
            update();
        }
    }

    private void update() {
        Collections.sort(list, ITEM_COMPARATOR);
        int n = listitems();
        for (int i = 0; i < n; i++) {
            listitem(i).c = itempos(i);
        }
    }

    @Override
    public void init(Map<String, Boolean> base) {
        for (Map.Entry<String, Boolean> entry : customlist.entrySet()) {
            additem(new Item(entry.getKey()));
        }

        update();
    }

    @Override
    public void put(String key, Boolean val) {
        Item item = getItem(key);
        if (item != null) {
            if (item.cb.a != val) {
                item.update(val);
            }
        } else {
            add(key);
        }
    }

    @Override
    public void remove(String key) {
        Item item = getItem(key);
        if (item != null) {
            list.remove(item);
        }
    }

    public class Item extends Widget {

        public final String name;
        private final CheckBox cb;
        private boolean a = false;
        private UI.Grab grab;

        public Item(String name) {
            super(new Coord(200, 25));
            this.name = name;

            cb = add(new CheckBox(name), 3, 3);
            cb.a = customlist.get(name);
            cb.canactivate = true;

            add(new Button(24, "X") {
                @Override
                public void click() {
                    super.wdgmsg("activate", name);
                }

                @Override
                public boolean mouseup(Coord c, int button) {
                    //FIXME:a little hack, because WidgetList does not pass correct click coordinates if scrolled
                    return super.mouseup(Coord.z, button);
                }
            }, 175, 0);
        }

        @Override
        public boolean mousedown(Coord c, int button) {
            if (super.mousedown(c, button)) {
                return true;
            }
            if (button != 1)
                return (false);
            a = true;
            grab = ui.grabmouse(this);
            return (true);
        }

        @Override
        public boolean mouseup(Coord c, int button) {
            if (a && button == 1) {
                a = false;
                if (grab != null) {
                    grab.remove();
                    grab = null;
                }
                if (c.isect(new Coord(0, 0), sz))
                    click();
                return (true);
            }
            return (false);
        }

        private void click() {
            cb.a = !cb.a;
            wdgmsg("changed", name, cb.a);
        }

        @Override
        public void wdgmsg(Widget sender, String msg, Object... args) {
            switch (msg) {
                case "ch":
                    wdgmsg("changed", name, (int) args[0] > 0);
                    break;
                case "activate":
                    wdgmsg("delete", name);
                    break;
                default:
                    super.wdgmsg(sender, msg, args);
                    break;
            }
        }

        public void update(boolean a) {
            this.cb.a = a;
        }
    }

    public boolean contains(String name) {
        for (Item item : list) {
            if (item.name.equals(name))
                return true;
        }
        return false;
    }

    public Item getItem(String name) {
        for (Item item : list) {
            if (item.name.equals(name))
                return item;
        }
        return null;
    }
}