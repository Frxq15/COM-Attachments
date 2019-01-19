package com.rhetorical.cod.progression;

import com.rhetorical.cod.ComVersion;
import com.rhetorical.cod.Main;
import com.rhetorical.cod.files.ProgressionFile;
import com.rhetorical.cod.lang.Lang;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;

public class ProgressionManager {

	private HashMap<Player, Integer> prestigeLevel = new HashMap<>();
	// Start from 0

	private HashMap<Player, Integer> level = new HashMap<>();
	// Start from 1

	private HashMap<Player, Double> experience = new HashMap<>();
	// Start from 0

	public final int maxLevel;
	private final int maxPrestigeLevel;

	public ProgressionManager() {

		for (Player p : Bukkit.getOnlinePlayers()) {
			this.loadData(p);
		}

		int maxLevelFromConfig = Main.getPlugin().getConfig().getInt("maxLevel");
		int maxPrestigeLevelFromConfig = ComVersion.getPurchased() ? Main.getPlugin().getConfig().getInt("maxPrestigeLevel") : 0;

		if (maxLevelFromConfig <= 0) {
			this.maxLevel = 55;
		} else {
			this.maxLevel = maxLevelFromConfig;
		}

		if (maxPrestigeLevelFromConfig < 0) {
			this.maxPrestigeLevel = 10;
		} else {
			this.maxPrestigeLevel = maxPrestigeLevelFromConfig;
		}

	}

	public void setLevel(Player p, int level, boolean showMessage) {
		if (level > getLevel(p)) {
			for (int i = getLevel(p) + 1; i <= level; i++) {
				this.level.put(p, i);
				Main.shopManager.checkForNewGuns(p);
			}
		} else {
			this.level.put(p, level);
		}
		if (showMessage) {
			p.sendMessage(Main.codPrefix + Lang.RANK_UP_MESSAGE.getMessage().replace("{level}", getLevel(p) + ""));
		}

	}

	private void addLevel(Player p) {
		this.setExperience(p, 0);
		if (!this.level.containsKey(p)) {
			this.level.put(p, 1);
		}

		this.level.put(p, this.level.get(p) + 1);
		p.sendMessage(Main.codPrefix + Lang.RANK_UP_MESSAGE.getMessage().replace("{level}", getLevel(p) + ""));

		if (this.getLevel(p) == this.maxLevel) {
			p.sendMessage(Main.codPrefix + Lang.RANK_UP_READY_TO_PRESTIGE.getMessage());
		}

		Main.shopManager.checkForNewGuns(p);
	}

	public int getLevel(Player p) {
		if (!this.level.containsKey(p)) {
			this.level.put(p, 1);
		}

		return this.level.get(p);
	}

	public void setPrestigeLevel(Player p, int level, boolean showMessage) {
		this.prestigeLevel.put(p, level);

		if (showMessage) {
			p.sendMessage(Main.codPrefix + Lang.RANK_UP_PRESTIGE_MESSAGE.getMessage().replace("{level}", getPrestigeLevel(p) + ""));
		}

	}

	public boolean addPrestigeLevel(Player p) {
		if (!prestigeLevel.containsKey(p)) {
			prestigeLevel.put(p, 0);
		}

		if (getPrestigeLevel(p) >= maxPrestigeLevel) {
			Main.sendMessage(p, Lang.ALREADY_HIGHEST_PRESTIGE.getMessage(), Main.lang);
			return false;
		}

		this.prestigeLevel.put(p, prestigeLevel.get(p) + 1);
		setExperience(p, 0d);
		setLevel(p, 1, false);

		Main.shopManager.prestigePlayer(p);


		p.sendMessage(Main.codPrefix + Lang.RANK_UP_PRESTIGE_MESSAGE.getMessage().replace("{level}", getPrestigeLevel(p) + ""));
		p.sendMessage(Main.codPrefix + Lang.RANK_RESET_MESSAGE.getMessage());
		return true;
	}

	public int getPrestigeLevel(Player p) {
		if (!this.prestigeLevel.containsKey(p)) {
			this.prestigeLevel.put(p, 0);
		}

		return this.prestigeLevel.get(p);
	}

	private void setExperience(Player p, double experience) {
		this.experience.put(p, experience);
		update(p);
		StatHandler.addExperience(p, experience - StatHandler.getExperience(p.getName()));
	}

	public void addExperience(Player p, double experience) {
		int level = getLevel(p);

		if (level == maxLevel)
			return;

		double requiredExperience = getExperienceForLevel(level);

		double current = getExperience(p) + experience;

		StatHandler.addExperience(p, experience);

		if (current >= requiredExperience) {
			double difference = current - requiredExperience;
			if (this.getLevel(p) < this.maxLevel) {
				addLevel(p);
			}
			if (difference != 0d)
				addExperience(p, difference);
		} else {
			setExperience(p, current);
		}
		update(p);
	}

	private double getExperience(Player p) {
		if (!this.experience.containsKey(p)) {
			this.experience.put(p, 0D);
		}

		return this.experience.get(p);
	}

	private double getExperienceForLevel(int level) {

//		if (level <= 0) {
//			return 0D;
//		} else if (level == 1) {
//			return 400D;
//		} else if (level == 2) {
//			return 4000D;
//		} else if (level < 10) {
//			exp = 4000 + (600 * (level - 2));
//		} else if (level > 10 && level <= 31) {
//			exp = 4000 + (800 * level - 2);
//		} else if (level >= 31 && level <= 55) {
//			exp = 4000 + (1200 * level - 2);
//		} else {
//			exp = 4000 + (2000 * level - 2);
//		}

		return (level * 120d) + 240;
	}

	public void update(Player p) {

		try {
			p.setExp((float) (getExperience(p) / getExperienceForLevel(getLevel(p))));
		} catch (Exception e) {
			Main.sendMessage(Main.cs, Lang.ERROR_SETTING_PLAYER_EXPERIENCE_LEVEL.getMessage(), Main.lang);
		}
	}

	public void loadData(Player p) {
		int k = 0;
		while (ProgressionFile.getData().contains("Players." + k)) {

			if (p == Bukkit.getPlayer(ProgressionFile.getData().getString("Players." + k + ".name"))) {
				int playerLevel = ProgressionFile.getData().getInt("Players." + k + ".level");
				double playerExperience = ProgressionFile.getData().getDouble("Players." + k + ".experience");
				int playerPrestigeLevel = ProgressionFile.getData().getInt("Players." + k + ".prestigeLevel");

				this.level.put(p, playerLevel);
				this.experience.put(p, playerExperience);
				this.prestigeLevel.put(p, playerPrestigeLevel);

				return;
			}

			k++;
		}

	}

	public void saveData(Player p) {

		int k;
		for (k = 0; ProgressionFile.getData().contains("Players." + k); k++) {
			if (Bukkit.getPlayer(ProgressionFile.getData().getString("Players." + k + ".name")) == p) {
				ProgressionFile.getData().set("Players." + k + ".level", getLevel(p));
				ProgressionFile.getData().set("Players." + k + ".experience", getExperience(p));
				ProgressionFile.getData().set("Players." + k + ".prestigeLevel", getPrestigeLevel(p));
				ProgressionFile.saveData();
				ProgressionFile.reloadData();
				return;
			}
		}

		ProgressionFile.getData().set("Players." + k + ".name", p.getName());
		ProgressionFile.getData().set("Players." + k + ".level", getLevel(p));
		ProgressionFile.getData().set("Players." + k + ".experience", getExperience(p));
		ProgressionFile.getData().set("Players." + k + ".prestigeLevel", getPrestigeLevel(p));
		ProgressionFile.saveData();
		ProgressionFile.reloadData();
	}

	public ArrayList<Player> getPlayerRankings() {

		return new ArrayList<>();
	}

}