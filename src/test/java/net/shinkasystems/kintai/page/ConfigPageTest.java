/**
 * 
 */
package net.shinkasystems.kintai.page;

import java.io.File;
import java.time.LocalDate;
import java.util.Locale;

import net.shinkasystems.kintai.KintaiDB;
import net.shinkasystems.kintai.KintaiRole;
import net.shinkasystems.kintai.KintaiSession;
import net.shinkasystems.kintai.KintaiWebApplication;
import net.shinkasystems.kintai.entity.User;

import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.dbunit.IDatabaseTester;
import org.dbunit.JdbcDatabaseTester;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.csv.CsvDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Aogiri
 *
 */
public class ConfigPageTest {

	/**
	 * Wicket のテスターです。
	 */
	private WicketTester wicketTester;

	/**
	 * データベースのテスターです。
	 */
	private IDatabaseTester databaseTester;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		/*
		 * Locale
		 */
		Locale.setDefault(Locale.JAPANESE);

		/*
		 * Application
		 */
		KintaiDB.setMode(KintaiDB.TEST); // テストモードで DB 接続を行う
		KintaiDB.createDB();

		/*
		 * Wicket
		 */
		wicketTester = new WicketTester(new KintaiWebApplication());

		/*
		 * DBUnit
		 */
		databaseTester = new JdbcDatabaseTester(
				"org.h2.Driver",
				"jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
				"sa", // user
				null, // pass
				null // schema
		);

		databaseTester.setDataSet(new CsvDataSet(
				new File("src/test/resources/META-INF/net/shinkasystems/kintai/page/ConfigPageTest")));
		databaseTester.setSetUpOperation(DatabaseOperation.CLEAN_INSERT);
		databaseTester.onSetup();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {

		/*
		 * Wicket
		 */
		wicketTester.destroy();

		/*
		 * DBUnit
		 */
		databaseTester.setTearDownOperation(DatabaseOperation.DELETE);
		databaseTester.onTearDown();
	}

	/**
	 * テスト用にログインします。
	 */
	private static void login() {

		KintaiSession kintaiSession = (KintaiSession) KintaiSession.get();

		User user = new User();
		user.setActivated(true);
		user.setAuthorityId(2);
		user.setDisplayName("テスト　太郎");
		user.setEmailAddress("test@localhost");
		user.setExpireDate(LocalDate.now());
		user.setId(1006);
		user.setPassword("test");
		user.setRole(KintaiRole.USER);
		user.setUserName("test");

		Roles roles = new Roles(KintaiRole.USER);

		kintaiSession.setUser(user);
		kintaiSession.setRoles(roles);
		kintaiSession.signInForTest();
	}

	/**
	 * ユーザーの各種設定画面が表示されること。
	 */
	@Test
	public void testPageRender() {

		login();

		wicketTester.startPage(ConfigPage.class);

		wicketTester.assertRenderedPage(ConfigPage.class);
	}

	/**
	 * 設定画面のコンポーネントが表示されること。
	 */
	@Test
	public void testComponentRender() {

		login();

		wicketTester.startPage(ConfigPage.class);

		wicketTester.assertComponent("config-form", Form.class);
		wicketTester.assertComponent("config-form:password", PasswordTextField.class);
		wicketTester.assertComponent("config-form:password-confirm", PasswordTextField.class);
	}

	/**
	 * パスワード変更に成功すること。
	 * @throws Exception 
	 */
	@Test
	public void testChangePasswordSuccess() throws Exception {

		// ログイン
		login();

		// 設定画面
		wicketTester.startPage(ConfigPage.class);

		FormTester formTester = wicketTester.newFormTester("config-form");

		formTester.setValue("password", "new_password");
		formTester.setValue("password-confirm", "new_password");

		formTester.submit();

		// 期待値データを読み込み
		IDataSet expectedDataSet = new FlatXmlDataSetBuilder()
				.build(
				new File(
						"src/test/resources/META-INF/net/shinkasystems/kintai/page/USER_PASSWORD_CHANGE_SUCCESS.xml"));

		// 実際のDBの値を取得
		IDataSet actualDataSet = databaseTester.getConnection().createDataSet();

		// 比較
		Assert.assertEquals(
				expectedDataSet.getTable("USER").getValue(0, "password"),
				actualDataSet.getTable("USER").getValue(0, "password"));
	}
}
