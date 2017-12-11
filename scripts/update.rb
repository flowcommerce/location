#!/usr/bin/env ruby

# Usage:
# ./scripts/update.rb 0538 ./na_04_db.tar.gz

DOC_URL = "https://www.notion.so/flow/Updating-DigitalElement-Edge-Database-ce2c3836ec9e4121bb14fdb0f3d4cd53"
CONFIG_PATH = "api/conf/application.production.conf"

if !File.exists?(CONFIG_PATH)
  puts "ERROR: Cannod find configuration file: #{CONFIG_PATH}"
  puts "       Please run from location project root directory."
  exit(1)
end
  
version = ARGV.shift.to_s.strip
tarball = ARGV.shift.to_s.strip

if version.empty?
  puts "ERROR: Please provide version"
  exit(1)
elsif version.to_i.to_s != version.sub(/^0+/, '')
  puts "ERROR: Expected version '#{version}' to be numeric (e.g. 0538)"
  exit(1)
elsif version.to_i < 500
  puts "ERROR: Expected version '#{version}' to > 0500"
  exit(1)
end

if tarball.empty?
  puts "ERROR: Please provide path to database tarball (e.g. 04.tar.gz)"
  exit(1)
end

if !tarball.match(/\.tar.gz$/)
  puts "ERROR: Path '#{tarball}' must end with .tar.gz"
  exit(1)
end

if !File.exists?(tarball)
  puts "ERROR: Path '#{tarball}' does not exit"
  exit(1)
end

if !system("which netacuity-textfile-creator.sh")
  puts "Please install netacuity-textfile-creator.sh"
  puts "See #{DOC_URL}"
  exit(1)
end
binary = `which netacuity-textfile-creator.sh`.strip

def run(cmd)
  puts "==> #{cmd}"
  if !system(cmd)
    puts "ERROR: Command failed: #{cmd}"
    exit(1)
  end
end

def update_config(path, new_version)
  key = "digitalelement.file.uri"
  tmp = File.basename(path)
  found = false
  File.open(tmp, "w") do |out|
    IO.readlines(path).each do |l|
      name, value = l.split(/=/, 2)

      if name.strip == key
        found = true
        out << l.sub(/\d+\.csv/, "#{new_version}.csv")
      else
        out << l
      end
    end
  end

  if !found
    puts "ERROR: Could not find key '#{key}' in configuration file '#{path}'"
    exit(1)
  end

  run "cp #{tmp} #{path}"
end

pwd = `pwd`.strip

tmp = File.join("/tmp", "location-update.#{Process.pid}")
run("mkdir #{tmp}")

target = "#{version}.csv"
Dir.chdir(tmp) do
  update_config(File.join(pwd, CONFIG_PATH), version)
  run "tar xvfz #{File.join(pwd, tarball)}"
  run "#{binary} --db_path=. --db=4 --numeric --fields=edge-country,edge-region,edge-city,edge-latitude,edge-longitude,edge-postal-code --output_file=./#{target}"
  run "#{binary} --db_path=. --db=4 --ipv6 --numeric --fields=edge-country,edge-region,edge-city,edge-latitude,edge-longitude,edge-postal-code --output_file=./#{version}.ipv6.csv"
  run "cut -f 1,3,5,6,7,8,9,10,11 -d ';' ./#{version}.ipv6.csv >> ./#{target}"
  run "aws s3 cp ./#{target} s3://io-flow-location/digitalelement/edge/#{target} --grants read=uri=http://acs.amazonaws.com/groups/global/AllUsers"
end

branch = "digital_element_upgrade_#{version}"
msg = "Upgrade digital element db to version #{version}"
run("git checkout -b #{branch}")
run("git commit -m '#{msg}' #{CONFIG_PATH}")
run("git push origin #{branch}")
run("hub pull-request -m '#{msg}'")
run("git checkout master")
run("git branch -D #{branch}")
