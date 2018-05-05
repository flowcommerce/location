#!/usr/bin/env ruby

# this is a quick script to read one of our csv files (e.g. 0562.csv)
# load the data set in memory
# then find the lines that are in range for specific big int IPs

class El
  attr_reader :min, :max, :line
  def initialize(min,max, line)
    @min = min
    @max = max
    @line = line
  end
end


desired = [633037714, 386924544, 3119302755, 2906605499]

desired_min = desired.min
desired_max = desired.max

data = []
count = 0
IO.readlines("0562.csv").each do |l|
#IO.readlines("sample.csv").each do |l|
  count += 1
  if count % 500000 == 0
    puts " - Read #{count} lines"
  end
  min, max, rest = l.split(/;/, 3).map(&:to_i)
  if min >= desired_min
    data << El.new(min, max, l)
  end
end
puts "Done reading file\n"

lines = []
desired.each do |ip|
  el = data.find { |el|
    el.min <= ip && el.max > ip
  }
  if el
    puts "#{ip} => #{el.min};#{el.max}"
    lines << el.line
  else
    puts "#{ip} => not found"
  end
end

puts ""
puts lines.join("\n\n")
