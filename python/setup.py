#!/usr/bin/env/ python
# -*- coding: utf-8 -*-

import re
from setuptools import setup, find_packages  # type: ignore
from typing import List


requirements: List[str] = []

with open("audit_middleware/__init__.py", "r") as f:
    version = re.search(
        r'^__version__\s*=\s*[\'"]([^\'"]*)[\'"]',  # type: ignore
        f.read(),
        re.MULTILINE,
    ).group(1)

setup(
    name="audit_middleware",
    version=version,
    author="Blackfynn, Inc.",
    author_email="help@blackfynn.com",
    description="A library for generating audit log events on the Blackfynn Platform (internal only)",
    packages=find_packages(),
    package_dir={"audit_middleware": "audit_middleware"},
    install_requires=requirements,
    license="",
    classifiers=["Development Status :: 3 - Alpha", "Topic :: Utilities"],
)
