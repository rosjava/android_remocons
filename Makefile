clean:
	@for i in *; do \
	  if [ -d "$${i}/build" ]; then \
            echo "++ $${i}"; \
	    rm -rf $${i}/build; \
	  else \
	    echo "-- $${i}"; \
          fi; \
	done

