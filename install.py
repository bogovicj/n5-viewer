#!/usr/bin/env python

import os
import sys
import subprocess

if __name__ == '__main__':
	fiji_dir = sys.argv[1]
	misc_args = sys.argv[2:]
	base_folder = os.path.dirname(os.path.abspath(__file__))
	cmd_args = ['mvn', '-Dimagej.app.directory=' + fiji_dir, 'clean', 'install'] + misc_args
	subprocess.call(cmd_args, cwd=base_folder)