package net.tbnr.announcer.announces.stores;

import net.tbnr.announcer.announces.Announcement;
import net.tbnr.announcer.announces.AnnouncementStorage;

import java.util.List;

/**
 * Created by Joey on 2/16/14.
 */
public class MySQLStorage implements AnnouncementStorage {
    @Override
    public List<Announcement> getAllAnnouncements() {
        return null;
    }

    @Override
    public void saveAnnouncement(Announcement announcement) {

    }

    @Override
    public void removeAnnouncement(Announcement announcement) {

    }

    @Override
    public Integer getInterval() {
        return null;
    }

    @Override
    public void setInterval(Integer interval) {

    }
}