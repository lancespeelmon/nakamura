#!/usr/bin/env ruby

require 'sling/sling'
require 'sling/test'
require 'sling/contacts'
require 'test/unit.rb'
require 'test/unit/ui/console/testrunner.rb'
include SlingInterface
include SlingUsers
include SlingContacts

class TC_Kern280Test < Test::Unit::TestCase
  include SlingTest

  def setup
    super
    @cm = ContactManager.new(@s)
  end


  def test_connect_non_existant_user
    m = Time.now.to_i.to_s
    u = create_user("testuser"+m)
    @s.switch_user(u)
    res = @cm.invite_contact("nonexisant_user"+m, [ "coworker", "friend" ])
    assert_equal("404", res.code, "User should not have existed")
  end

end


