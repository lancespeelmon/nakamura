#!/usr/bin/env ruby

require 'rubygems'
require 'bundler'
Bundler.setup(:default)
require 'nakamura/test'
require 'nakamura/users'
require 'test/unit.rb'

include SlingUsers

class TC_Kern3052 < Test::Unit::TestCase
  include SlingTest


  def setup
    super
    @um = UserManager.new(@s)
  end

  def test_user_id_searches
    m = uniqueness()
    userid = "Jean-Ren\u00e9.#{m}"
    res = @s.execute_post(@s.url_for("system/userManager/user.create.html"), {
        ":name" => userid,
        "pwd" => "testuser",
        "pwdConfirm" => "testuser",
        "_charset_" => "utf-8",
	"email" => "#{userid}@foo.com"
    })
    assert_equal("201", res.code)
    wait_for_indexer

    # Exact match should succeed.
    res = @s.execute_get(@s.url_for("system/userManager/user.exists.html"), {
        "userid" => userid,
        "_charset_" => "utf-8"
    })
    assert_equal("204", res.code)

    # Case differences should succeed.
    res = @s.execute_get(@s.url_for("system/userManager/user.exists.html"), {
        "userid" => "jean-ren\u00e9.#{m}",
        "_charset_" => "utf-8"
    })
    assert_equal("204", res.code)

    # Whitespace should be ignored.
    res = @s.execute_get(@s.url_for("system/userManager/user.exists.html"), {
        "userid" => " #{userid} ",
        "_charset_" => "utf-8"
    })
    assert_equal("204", res.code)

    # Substrings should not match.
    res = @s.execute_get(@s.url_for("system/userManager/user.exists.html"), {
        "userid" => "Jean",
        "_charset_" => "utf-8"
    })
#this is actually an error @{m} comes across as literally the characters "@{m}"
#this test is being kept, however, as it identified a failure to escape Solr search chars in LiteUserExistsServlet!
    assert_equal("404", res.code)
    res = @s.execute_get(@s.url_for("system/userManager/user.exists.html"), {
        "userid" => "@{m}",
        "_charset_" => "utf-8"
    })
#this test does what, I believe, the previous test was *intended* to do
    assert_equal("404", res.code)
    res = @s.execute_get(@s.url_for("system/userManager/user.exists.html"), {
        "userid" => "#{m}",
        "_charset_" => "utf-8"
    })
    assert_equal("404", res.code)
  end
end
