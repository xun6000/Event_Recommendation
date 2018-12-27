package db.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import db.DBConnection;
import entity.Item;
import external.ExternalAPI;
import external.ExternalAPIFactory;


public class MySQLConnection implements DBConnection {
	private static MySQLConnection instance;

	public static DBConnection getInstance() {
		// lazy initialization
		if (instance == null) {
			instance = new MySQLConnection();
		}
		return instance;
	}

	// Import java.sql.Connection. Don't use com.mysql.jdbc.Connection.
	private Connection conn = null;

	private MySQLConnection() {
		try {
			// Forcing the class representing the MySQL driver to load and
			// initialize.
			// The newInstance() call is a work around for some broken Java
			// implementations.
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection(MySQLDBUtil.URL);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void close() {
		if (conn != null) {
			try {
				conn.close();
			} catch (Exception e) { /* ignored */
			}
		}
	}

	@Override
	public void setFavoriteItems(String userId, List<String> itemIds) {
		String query = "INSERT INTO history (user_id, item_id) VALUES (?, ?)";
		try {
			PreparedStatement statement = conn.prepareStatement(query);
			for (String itemId : itemIds) {
				statement.setString(1, userId);
				statement.setString(2, itemId);
				statement.execute();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void unsetFavoriteItems(String userId, List<String> itemIds) {
		String query = "DELETE FROM history WHERE user_id=? AND item_id = ?";
		try {
			PreparedStatement statement = conn.prepareStatement(query);
			for (String itemId : itemIds) {
				statement.setString(1, userId);
				statement.setString(2, itemId);
				statement.execute();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public Set<String> getFavoriteItemIds(String userId) {
		String query = "SELECT item_id FROM history WHERE user_id = ?";
		Set<String> itemIdSet = new HashSet<>();
		try {
			PreparedStatement statement = conn.prepareStatement(query);
			statement.setString(1, userId);
			ResultSet queryResult = statement.executeQuery();
			while (queryResult.next()) {
				itemIdSet.add(queryResult.getString(1));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return itemIdSet;
	}

	@Override
	public Set<Item> getFavoriteItems(String userId) {
		String query = "SELECT * FROM items WHERE item_id = ?";
		Set<Item> itemSet = new HashSet<>();
		Set<String> itemIdSet = getFavoriteItemIds(userId);
		for (String itemId : itemIdSet) {
			try {
				PreparedStatement statement = conn.prepareStatement(query);
				statement.setString(1, itemId);
				// query from items table
				ResultSet queryResult = statement.executeQuery();
				Item.ItemBuilder builder = new Item.ItemBuilder();
				// there should be only one result returned
				if (queryResult.next()) {
					builder.setItemId(queryResult.getString(1));
					builder.setName(queryResult.getString(2));
					builder.setCity(queryResult.getString(3));
					builder.setState(queryResult.getString(4));
					builder.setCountry(queryResult.getString(5));
					builder.setZipcode(queryResult.getString(6));
					builder.setRating(queryResult.getDouble(7));
					builder.setAddress(queryResult.getString(8));
					builder.setLatitude(queryResult.getDouble(9));
					builder.setLongitude(queryResult.getDouble(10));
					builder.setDescription(queryResult.getString(11));
					builder.setSnippet(queryResult.getString(12));
					builder.setSnippetUrl(queryResult.getString(13));
					builder.setImageUrl(queryResult.getString(14));
					builder.setUrl(queryResult.getString(15));
				}
				// query from categories table
				builder.setCategories(getCategories(itemId));
				itemSet.add(builder.build());
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return itemSet;
	}

	@Override
	public Set<String> getCategories(String itemId) {
		String query = "SELECT category FROM categories WHERE item_id = ?";
		Set<String> categories = new HashSet<>();
		try {
			PreparedStatement statement = conn.prepareStatement(query);
			statement.setString(1, itemId);
			ResultSet queryResult = statement.executeQuery();
			while (queryResult.next()) {
				categories.add(queryResult.getString(1));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return categories;
	}

	@Override
	public List<Item> searchItems(String userId, double lat, double lon, String term) {
		// Connect to external API
		ExternalAPI api = ExternalAPIFactory.getExternalAPI(); // moved here
		List<Item> items = api.search(lat, lon, term);
		for (Item item : items) {
			// Save the item into our own db.
			saveItem(item);
		}
		return items;

	}

	@Override
	public void saveItem(Item item) {
		try {
			// First, insert into items table
			String sql = "INSERT IGNORE INTO items VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, item.getItemId());
			statement.setString(2, item.getName());
			statement.setString(3, item.getCity());
			statement.setString(4, item.getState());
			statement.setString(5, item.getCountry());
			statement.setString(6, item.getZipcode());
			statement.setDouble(7, item.getRating());
			statement.setString(8, item.getAddress());
			statement.setDouble(9, item.getLatitude());
			statement.setDouble(10, item.getLongitude());
			statement.setString(11, item.getDescription());
			statement.setString(12, item.getSnippet());
			statement.setString(13, item.getSnippetUrl());
			statement.setString(14, item.getImageUrl());
			statement.setString(15, item.getUrl());
			statement.execute();

			// Second, update categories table for each category.
			sql = "INSERT IGNORE INTO categories VALUES (?,?)";
			for (String category : item.getCategories()) {
				statement = conn.prepareStatement(sql);
				statement.setString(1, item.getItemId());
				statement.setString(2, category);
				statement.execute();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
